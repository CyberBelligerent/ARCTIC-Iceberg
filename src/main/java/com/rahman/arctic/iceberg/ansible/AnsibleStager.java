package com.rahman.arctic.iceberg.ansible;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.rahman.arctic.iceberg.objects.RangeExercise;
import com.rahman.arctic.iceberg.objects.computers.ArcticHost;
import com.rahman.arctic.iceberg.repos.AnsibleRoleRepo;

@Service
public class AnsibleStager {

	private static final Path OUTPUT_ROOT = Paths.get("ansible-out");

	private final AnsibleRoleRepo roleRepo;

	// Key for hopping from provider network to Ansible Controller (Sending ansible configurations)
	@Value("${arctic.ansible.ssh-key-path:/app/secrets/id_arctic}")
	private String sshKeyPath;

	// Remote user baked into the ARCTICAnsibleController image at packer build time.
	@Value("${arctic.ansible.controller-user:ansible_prov}")
	private String controllerUser;

	// Key for hopping from Ansible Controller to range host
	@Value("${arctic.ansible.range-key-path:/home/ansible_prov/.ssh/id_arctic_range}")
	private String rangeKeyPath;

	@Autowired
	public AnsibleStager(AnsibleRoleRepo roleRepo) {
		this.roleRepo = roleRepo;
	}

	public void stage(RangeExercise exercise) {
		if (exercise == null) return;
		String folderName = sanitize(exercise.getName());
		Path exDir = OUTPUT_ROOT.resolve(folderName);
		try {
			wipe(exDir);
			Files.createDirectories(exDir);
			Files.createDirectories(exDir.resolve("roles"));
			Files.createDirectories(exDir.resolve("playbooks"));

			writeInventory(exDir, exercise);
			writeRoles(exDir, exercise);
			writeHostPlaybooks(exDir, exercise);
			writeSiteYml(exDir, exercise);
			writeAnsibleCfg(exDir);
			System.out.println("[AnsibleStager] staged " + exDir.toAbsolutePath());
		} catch (IOException e) {
			System.err.println("[AnsibleStager] failed to stage '" + exercise.getName() + "': " + e.getMessage());
			e.printStackTrace();
		}
	}

	public void cleanup(String exerciseName) {
		if (exerciseName == null || exerciseName.isBlank()) return;
		Path exDir = OUTPUT_ROOT.resolve(sanitize(exerciseName));
		try {
			wipe(exDir);
			System.out.println("[AnsibleStager] cleaned " + exDir.toAbsolutePath());
		} catch (IOException e) {
			System.err.println("[AnsibleStager] failed to clean '" + exerciseName + "': " + e.getMessage());
		}
	}

	// SCP the required files to the Ansible Controller
	public void pushAndRun(String controllerIp, String exerciseName) {
		if (controllerIp == null || controllerIp.isBlank()) {
			System.err.println("[AnsibleStager] pushAndRun skipped — no controller IP");
			return;
		}
		if (exerciseName == null || exerciseName.isBlank()) {
			System.err.println("[AnsibleStager] pushAndRun skipped — no exercise name");
			return;
		}

		String folderName = sanitize(exerciseName);
		Path exDir = OUTPUT_ROOT.resolve(folderName);
		if (!Files.exists(exDir)) {
			System.err.println("[AnsibleStager] pushAndRun skipped — " + exDir.toAbsolutePath() + " missing");
			return;
		}

		String remoteBase = "/home/" + controllerUser + "/arctic";
		String remoteExPath = remoteBase + "/" + folderName;
		String userAtHost = controllerUser + "@" + controllerIp;

		if (!waitForSshReady(controllerIp, 22, 300, 3)) {
			System.err.println("[AnsibleStager] pushAndRun skipped — " + controllerIp
					+ ":22 never became reachable");
			return;
		}

		try {
			// Create parent file and delete old scripts
			runProcess("[pushAndRun:mkdir]", new ProcessBuilder("ssh",
					"-i", sshKeyPath,
					"-o", "StrictHostKeyChecking=no",
					"-o", "UserKnownHostsFile=/dev/null",
					"-o", "BatchMode=yes",
					userAtHost,
					"rm -rf " + remoteExPath + " && mkdir -p " + remoteBase));

			runProcess("[pushAndRun:scp]", new ProcessBuilder("scp",
					"-i", sshKeyPath,
					"-o", "StrictHostKeyChecking=no",
					"-o", "UserKnownHostsFile=/dev/null",
					"-o", "BatchMode=yes",
					"-r",
					exDir.toAbsolutePath().toString(),
					userAtHost + ":" + remoteBase + "/"));

			runProcess("[pushAndRun:ansible]", new ProcessBuilder("ssh",
					"-i", sshKeyPath,
					"-o", "StrictHostKeyChecking=no",
					"-o", "UserKnownHostsFile=/dev/null",
					"-o", "BatchMode=yes",
					userAtHost,
					"cd " + remoteExPath + " && ansible-playbook -i inventory.ini site.yml"));

			System.out.println("[AnsibleStager] pushAndRun complete for '" + exerciseName + "' @ " + controllerIp);
		} catch (IOException | InterruptedException e) {
			if (e instanceof InterruptedException) Thread.currentThread().interrupt();
			System.err.println("[AnsibleStager] pushAndRun failed for '" + exerciseName + "': " + e.getMessage());
		}
	}

	// Waits until SSH is fully ready with 3 successful connects
	private boolean waitForSshReady(String host, int port, int timeoutSeconds, int requiredStreak) {
		long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
		int attempt = 0;
		int streak = 0;
		while (System.currentTimeMillis() < deadline) {
			attempt++;
			try (java.net.Socket s = new java.net.Socket()) {
				s.connect(new java.net.InetSocketAddress(host, port), 3000);
				streak++;
				if (streak >= requiredStreak) {
					System.out.println("[AnsibleStager] " + host + ":" + port + " ready after "
							+ attempt + " attempt(s), streak=" + streak);
					return true;
				}
			} catch (IOException e) {
				if (streak > 0) {
					System.out.println("[AnsibleStager] " + host + ":" + port
							+ " probe failed mid-streak (had " + streak + "), resetting");
				}
				streak = 0;
			}
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return false;
			}
		}
		return false;
	}

	private void runProcess(String tag, ProcessBuilder pb) throws IOException, InterruptedException {
		pb.redirectErrorStream(true);
		Process p = pb.start();
		try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()))) {
			String line;
			while ((line = r.readLine()) != null) System.out.println(tag + " " + line);
		}
		int exit = p.waitFor();
		if (exit != 0) {
			throw new IOException(tag + " exited " + exit + " (cmd=" + String.join(" ", pb.command()) + ")");
		}
	}

	private void writeInventory(Path exDir, RangeExercise exercise) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("[all:vars]\n");
		sb.append("ansible_user=").append(controllerUser).append("\n");
		sb.append("ansible_ssh_private_key_file=").append(rangeKeyPath).append("\n");
		sb.append("ansible_ssh_common_args=-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null\n");
		sb.append("\n[all]\n");
		for (ArcticHost host : sortedHosts(exercise)) {
			String ip = host.getIp();
			if (ip == null || ip.isBlank()) {
				sb.append("# ").append(host.getName()).append(" — no IP resolved at build time\n");
				continue;
			}
			sb.append(host.getName()).append(" ansible_host=").append(ip).append("\n");
		}
		Files.writeString(exDir.resolve("inventory.ini"), sb.toString());
	}

	private void writeRoles(Path exDir, RangeExercise exercise) throws IOException {
		Set<String> seen = new HashSet<>();
		for (ArcticHost host : exercise.getHosts()) {
			for (HostRoleAssignment a : host.getRoleAssignments()) {
				if (!seen.add(a.getRoleId())) continue;
				Optional<AnsibleRole> opt = roleRepo.findById(a.getRoleId());
				if (opt.isEmpty()) {
					System.err.println("[AnsibleStager] role id=" + a.getRoleId() + " not found — skipping");
					continue;
				}
				AnsibleRole role = opt.get();
				Path tasksDir = exDir.resolve("roles").resolve(sanitize(role.getName())).resolve("tasks");
				Files.createDirectories(tasksDir);
				Files.writeString(tasksDir.resolve("main.yml"), role.getContent() == null ? "" : role.getContent());
			}
		}
	}

	private void writeHostPlaybooks(Path exDir, RangeExercise exercise) throws IOException {
		Map<String, String> roleIdToName = roleNameCache();
		for (ArcticHost host : exercise.getHosts()) {
			List<OrderedEntry> entries = new ArrayList<>();
			for (HostRoleAssignment a : host.getRoleAssignments()) {
				String roleName = roleIdToName.get(a.getRoleId());
				if (roleName == null) continue;
				entries.add(OrderedEntry.forRole(a.getRunOrder(), roleName, a.getOverrideVariables()));
			}
			for (HostInlineScript s : host.getInlineScripts()) {
				entries.add(OrderedEntry.forScript(s.getRunOrder(), s.getName(), s.getContent()));
			}
			entries.sort(Comparator.comparingInt(e -> e.runOrder));

			StringBuilder sb = new StringBuilder();
			sb.append("- hosts: ").append(host.getName()).append("\n");
			sb.append("  become: true\n");
			if (entries.isEmpty()) {
				sb.append("  tasks: []\n");
			} else {
				sb.append("  tasks:\n");
				for (OrderedEntry e : entries) {
					sb.append(e.toYaml());
				}
			}
			Files.writeString(exDir.resolve("playbooks").resolve(sanitize(host.getName()) + ".yml"), sb.toString());
		}
	}

	private void writeAnsibleCfg(Path exDir) throws IOException {
		String cfg = "[defaults]\n"
				+ "collections_paths = /usr/share/ansible/collections:~/.ansible/collections\n"
				+ "roles_path        = ./roles\n"
				+ "host_key_checking = False\n";
		Files.writeString(exDir.resolve("ansible.cfg"), cfg);
	}

	private void writeSiteYml(Path exDir, RangeExercise exercise) throws IOException {
		StringBuilder sb = new StringBuilder();
		for (ArcticHost host : sortedHosts(exercise)) {
			sb.append("- import_playbook: playbooks/").append(sanitize(host.getName())).append(".yml\n");
		}
		Files.writeString(exDir.resolve("site.yml"), sb.toString());
	}

	private Map<String, String> roleNameCache() {
		Map<String, String> map = new HashMap<>();
		for (AnsibleRole r : roleRepo.findAll()) map.put(r.getId(), r.getName());
		return map;
	}

	private List<ArcticHost> sortedHosts(RangeExercise exercise) {
		List<ArcticHost> list = new ArrayList<>(exercise.getHosts());
		list.sort(Comparator.comparing(ArcticHost::getName));
		return list;
	}

	private void wipe(Path dir) throws IOException {
		if (!Files.exists(dir)) return;
		try (Stream<Path> walk = Files.walk(dir)) {
			walk.sorted(Comparator.reverseOrder()).forEach(p -> {
				try { Files.delete(p); } catch (IOException ignored) {}
			});
		}
	}

	private String sanitize(String name) {
		if (name == null) return "unnamed";
		return name.replaceAll("[^A-Za-z0-9._-]", "_");
	}

	private static final class OrderedEntry {
		final int runOrder;
		final boolean isRole;
		final String name;
		final String content;
		final Map<String, String> vars;

		private OrderedEntry(int runOrder, boolean isRole, String name, String content, Map<String, String> vars) {
			this.runOrder = runOrder;
			this.isRole = isRole;
			this.name = name;
			this.content = content;
			this.vars = vars;
		}

		static OrderedEntry forRole(int order, String roleName, Map<String, String> overrides) {
			return new OrderedEntry(order, true, roleName, null, overrides != null ? overrides : Map.of());
		}

		static OrderedEntry forScript(int order, String scriptName, String content) {
			return new OrderedEntry(order, false, scriptName, content, Map.of());
		}

		String toYaml() {
			StringBuilder sb = new StringBuilder();
			if (isRole) {
				sb.append("    - name: include role ").append(name).append("\n");
				sb.append("      include_role:\n");
				sb.append("        name: ").append(name).append("\n");
				if (!vars.isEmpty()) {
					sb.append("      vars:\n");
					for (Map.Entry<String, String> e : vars.entrySet()) {
						sb.append("        ").append(e.getKey()).append(": ")
								.append(yamlQuote(e.getValue())).append("\n");
					}
				}
			} else {
				sb.append("    - name: ").append(name == null ? "inline script" : name).append("\n");
				sb.append("      shell: |\n");
				String body = content == null ? "" : content;
				for (String line : body.split("\n", -1)) {
					sb.append("        ").append(line).append("\n");
				}
			}
			return sb.toString();
		}

		private static String yamlQuote(String v) {
			if (v == null) return "\"\"";
			return "\"" + v.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
		}
	}
}
