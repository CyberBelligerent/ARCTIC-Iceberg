package com.rahman.arctic.iceberg.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.rahman.arctic.iceberg.ansible.AnsibleStager;
import com.rahman.arctic.iceberg.objects.RangeExercise;
import com.rahman.arctic.iceberg.objects.computers.ArcticHost;
import com.rahman.arctic.iceberg.objects.computers.ArcticNetwork;
import com.rahman.arctic.iceberg.objects.computers.ArcticRouter;
import com.rahman.arctic.iceberg.objects.computers.ArcticSecurityGroup;
import com.rahman.arctic.iceberg.objects.computers.ArcticSecurityGroupRule;
import com.rahman.arctic.iceberg.objects.computers.ArcticVolume;
import com.rahman.arctic.iceberg.objects.computers.HostCollection;
import com.rahman.arctic.iceberg.repos.ArcticHostRepo;
import com.rahman.arctic.iceberg.repos.ArcticNetworkRepo;
import com.rahman.arctic.iceberg.repos.ArcticRouterRepo;
import com.rahman.arctic.iceberg.repos.ArcticSecurityGroupRepo;
import com.rahman.arctic.iceberg.repos.ArcticVolumeRepo;
import com.rahman.arctic.iceberg.repos.ExerciseRepo;
import com.rahman.arctic.iceberg.repos.HostCollectionRepo;
import com.rahman.arctic.shard.ShardManager;
import com.rahman.arctic.shard.configuration.persistence.ShardProfile;
import com.rahman.arctic.shard.objects.ArcticTask;
import com.rahman.arctic.shard.objects.abstraction.ArcticHostSO;
import com.rahman.arctic.shard.objects.abstraction.ArcticNetworkSO;
import com.rahman.arctic.shard.objects.abstraction.ArcticRouterSO;
import com.rahman.arctic.shard.objects.abstraction.ArcticSecurityGroupRuleSO;
import com.rahman.arctic.shard.objects.abstraction.ArcticSecurityGroupSO;
import com.rahman.arctic.shard.objects.abstraction.ArcticVolumeSO;
import com.rahman.arctic.shard.util.ARCTICLog;
import com.rahman.arctic.shard.util.IpUtil;

import lombok.Getter;
import lombok.Setter;

@Component
@Scope("prototype")
public class IcebergCreator extends Thread {
	
	private final ShardManager sm;
	private final ArcticNetworkRepo networkRepo;
	private final ArcticHostRepo hostRepo;
	private final ArcticRouterRepo routerRepo;
	private final ArcticSecurityGroupRepo securityGroupRepo;
	private final ArcticVolumeRepo volumeRepo;
	private final ExerciseRepo exerciseRepo;
	private final HostCollectionRepo collectionRepo;
	private final AnsibleStager ansibleStager;
	private ExecutorService executorService = Executors.newFixedThreadPool(5);

	@Getter @Setter
	public ShardProfile profile;

	@Getter @Setter
	private boolean destroyMode = false;

	@Getter @Setter
	private RangeExercise exercise;

	@Getter @Setter
	private String deploymentId;

	// Must be set for Ansible Controllers buildHost function wrapped in the wait()
	@Getter
	private volatile String controllerIp;

	// Hostname used for the per-range Ansible controller
	private String controllerName;

	@Getter
	private List<ArcticTask<?, ?>> tasksToComplete = new ArrayList<>();

	public IcebergCreator(ShardManager shardManager, ArcticNetworkRepo networkRepo, ArcticHostRepo hostRepo,
			ArcticRouterRepo routerRepo, ArcticSecurityGroupRepo securityGroupRepo, ArcticVolumeRepo volumeRepo,
			ExerciseRepo exerciseRepo, HostCollectionRepo collectionRepo, AnsibleStager ansibleStager) {
		sm = shardManager;
		this.networkRepo = networkRepo;
		this.hostRepo = hostRepo;
		this.routerRepo = routerRepo;
		this.securityGroupRepo = securityGroupRepo;
		this.volumeRepo = volumeRepo;
		this.exerciseRepo = exerciseRepo;
		this.collectionRepo = collectionRepo;
		this.ansibleStager = ansibleStager;
	}
	
	public void attemptCreation() {
		sm.createSession(profile);
	}
	
	public void createHostCollection(HostCollection hc) {
		validateIpBoundsForCollection(hc);

		ArcticHostSO ahso = new ArcticHostSO();
		ahso.setName(hc.getName());
		ahso.setRangeId(hc.getRangeId());
		ahso.setOsType(hc.getOsType());
		ahso.setVolumes(new java.util.HashSet<>(hc.getVolumes()));
		ahso.setCount(Math.max(1, hc.getCount()));
		ahso.setCollectionId(hc.getId());
		ahso.setExtraVariables(new java.util.HashMap<>(hc.getExtraVariables()));

		java.util.Set<String> resolvedNetNames = new java.util.HashSet<>();
		for (String netId : hc.getNetworks()) {
			java.util.Optional<ArcticNetwork> found = networkRepo.findById(netId);
			found.ifPresentOrElse(
				n -> resolvedNetNames.add(n.getNetName()),
				() -> resolvedNetNames.add(netId)
			);
		}
		ahso.setNetworks(resolvedNetNames);

		java.util.List<ArcticHostSO> instanceSos = sm.getSession(profile).createHostCollection(ahso);

		// Wipe stale instance rows so a redeploy doesn't accumulate ghosts.
		hc.getInstances().clear();
		collectionRepo.save(hc);

		for (int i = 0; i < instanceSos.size(); i++) {
			final int instanceIndex = i;
			final ArcticHostSO instanceSo = instanceSos.get(i);
			ArcticTask<?, ?> task = sm.getSession(profile).getInstanceTasks().get(instanceSo.getName());
			if (task == null) continue;
			task.setOnPersist(r -> {
				ArcticHost instance = new ArcticHost();
				instance.setCollectionId(hc.getId());
				instance.setRangeId(hc.getRangeId());
				instance.setInstanceIndex(instanceIndex);
				instance.setName(instanceSo.getName());
				instance.setIp(instanceSo.getIp());
				instance.setProviderId(instanceSo.getProviderId());
				instance.setBuilt(true);
				// Fan-out workers all complete on different pool threads and persist into the
				// same shared hc reference. The merge cascade iterates hc.instances while
				// another thread is mutating it → ConcurrentModificationException. Serialize.
				synchronized (hc) {
					hostRepo.save(instance);
					hc.getInstances().add(instance);
					collectionRepo.save(hc);
				}
			});
		}
	}

	/**
	 * For each entry in `network_ips`, verify start+(count-1) lies within the matching
	 * ArcticNetwork's CIDR. External networks (not in range.getNetworks()) are skipped + warned.
	 * Hard-fails by throwing IllegalArgumentException.
	 */
	private void validateIpBoundsForCollection(HostCollection hc) {
		int count = Math.max(1, hc.getCount());
		if (count == 1) return;

		String raw = hc.getExtraVariables().get("network_ips");
		if (raw == null || raw.isBlank()) return;

		java.util.Map<String, ArcticNetwork> netByName = new java.util.HashMap<>();
		if (exercise != null) {
			for (ArcticNetwork n : exercise.getNetworks()) {
				if (n.getNetName() != null) netByName.put(n.getNetName(), n);
			}
		}

		for (String line : raw.split("\n")) {
			String trimmed = line.trim();
			if (trimmed.isEmpty()) continue;
			int eq = trimmed.indexOf('=');
			if (eq <= 0) continue;
			String netName = trimmed.substring(0, eq).trim();
			String startIp = trimmed.substring(eq + 1).trim();

			ArcticNetwork managed = netByName.get(netName);
			if (managed == null || managed.getNetCidr() == null || managed.getNetCidr().isBlank()) {
				ARCTICLog.print("IcebergCreator", "WARN: network '" + netName
						+ "' is external (no CIDR). Skipping bounds check for collection '"
						+ hc.getName() + "'.");
				continue;
			}

			String lastIp;
			try {
				lastIp = IpUtil.increment(startIp, count - 1);
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("[" + hc.getName() + "] invalid start IP '"
						+ startIp + "' for network '" + netName + "'", e);
			}
			if (!IpUtil.isInCidr(startIp, managed.getNetCidr()) || !IpUtil.isInCidr(lastIp, managed.getNetCidr()))
				throw new IllegalArgumentException("[" + hc.getName() + "] IP range "
						+ startIp + "→" + lastIp + " escapes CIDR " + managed.getNetCidr()
						+ " on network '" + netName + "'");
		}
	}


	// Creates a per-range Ansible Controller
	public void createAnsibleController(RangeExercise ex) {
		if (ex == null) return;
		controllerName = "arctic-controller-" + ex.getName();

		ArcticHostSO ahso = new ArcticHostSO();
		ahso.setName(controllerName);
		ahso.setRangeId(ex.getId());
		// Build after host instances so explicit fan-out IPs are claimed first;
		// the controller takes whatever address IPAM has left.
		ahso.setPriorityOverride(20);
		java.util.Set<String> netNames = new java.util.HashSet<>();
		for (ArcticNetwork n : ex.getNetworks()) {
			if (n.getNetName() != null && !n.getNetName().isBlank()) netNames.add(n.getNetName());
		}
		ahso.setNetworks(netNames);

		java.util.Map<String, String> vars = new java.util.HashMap<>();
		vars.put("vm_template_name", "ARCTICAnsibleController");
		
		// Create provider_network (Really just a measure to ensure some standardization)
		// this is supplied for the MAAS installation of ARCTIC.
		// TODO: Either put this in docs it needs to be made or make it configurable...
		vars.put("provider_network_names", "provider_network");
		ahso.setExtraVariables(vars);

		sm.getSession(profile).createHost(ahso);

		ArcticTask<?, ?> task = sm.getSession(profile).getInstanceTasks().get(controllerName);
		if (task != null) task.setOnPersist(r -> {
			controllerIp = ahso.getIp();
			ex.setControllerProviderId(ahso.getProviderId());
			exerciseRepo.save(ex);
			ARCTICLog.print("IcebergCreator", "Ansible controller IP=" + controllerIp
					+ " providerId=" + ahso.getProviderId());
		});
	}

	public void createNetwork(ArcticNetwork an) {
		ArcticNetworkSO anso = new ArcticNetworkSO();
		anso.setName(an.getNetName());
		anso.setIpCidr(an.getNetCidr());
		anso.setIpGateway(an.getGateway());
		anso.setIpRangeEnd(an.getNetEnd());
		anso.setIpRangeStart(an.getNetStart());
		anso.setRangeId(an.getRangeId());
		anso.setExtraVariables(an.getExtraVariables());

		sm.getSession(profile).createNetwork(anso);

		ArcticTask<?, ?> task = sm.getSession(profile).getNetworkTasks().get(an.getNetName());
		if (task != null) task.setOnPersist(r -> {
			an.setProviderId(anso.getProviderId());
			an.setBuilt(true);
			networkRepo.save(an);
		});
	}
	
	public void createSecurityGroup(ArcticSecurityGroup asg) {
		ArcticSecurityGroupSO asgso = new ArcticSecurityGroupSO();
		asgso.setDescription(asg.getDescription());
		asgso.setName(asg.getName());
		asgso.setRangeId(asg.getRangeId());
		
		sm.getSession(profile).createSecurityGroup(asgso);

		ArcticTask<?, ?> task = sm.getSession(profile).getSecurityGroupTasks().get(asg.getName());
		if (task != null) task.setOnPersist(r -> {
			asg.setProviderId(asgso.getProviderId());
			securityGroupRepo.save(asg);
		});
	}
	
	public void createSecurityGroupRule(ArcticSecurityGroupRule asgr) {
		ArcticSecurityGroupRuleSO asgrso = new ArcticSecurityGroupRuleSO();
		asgrso.setDescription(asgr.getDescription());
		asgrso.setDirection(asgr.getDirection());
		asgrso.setEndPortRange(asgr.getEndPortRange());
		asgrso.setEth(asgr.getEth());
		asgrso.setName(asgr.getName());
		asgrso.setProtocol(asgr.getProtocol());
		asgrso.setRangeId(asgr.getRangeId());
		asgrso.setSecGroup(asgr.getSecGroup());
		asgrso.setStartPortRange(asgr.getStartPortRange());
		
		sm.getSession(profile).createSecurityGroupRule(asgrso);
	}
	
	public void createRouter(ArcticRouter ar) {
		ArcticRouterSO arso = new ArcticRouterSO();
		java.util.Set<String> resolvedNetNames = new java.util.HashSet<>();
		ARCTICLog.print("createRouter", "'" + ar.getName() + "' ar.getNetworks()=" + ar.getNetworks()
				+ "  repoCount=" + networkRepo.count());
		for (String netId : ar.getNetworks()) {
			java.util.Optional<ArcticNetwork> found = networkRepo.findById(netId);
			ARCTICLog.print("createRouter", "  findById('" + netId + "') → "
					+ (found.isPresent() ? "netName=" + found.get().getNetName() : "EMPTY"));
			found.ifPresentOrElse(
				n -> resolvedNetNames.add(n.getNetName()),
				() -> resolvedNetNames.add(netId)
			);
		}
		ARCTICLog.print("createRouter", "'" + ar.getName() + "' resolvedNetNames=" + resolvedNetNames);
		arso.setConnectedNetworkNames(resolvedNetNames);
		arso.setName(ar.getName());
		arso.setRangeId(ar.getRangeId());
		arso.setExtraVariables(ar.getExtraVariables());

		sm.getSession(profile).createRouter(arso);

		ArcticTask<?, ?> task = sm.getSession(profile).getRouterTasks().get(ar.getName());
		if (task != null) task.setOnPersist(r -> {
			ar.setProviderId(arso.getProviderId());
			ar.setBuilt(true);
			routerRepo.save(ar);
		});
	}
	
	public void createVolume(ArcticVolume av) {
		ArcticVolumeSO avso = new ArcticVolumeSO();
		avso.setBootable(av.isBootable());
		avso.setDescription(av.getDescription());
		avso.setImageId(av.getImageId());
		avso.setName(av.getName());
		avso.setRangeId(av.getRangeId());
		avso.setSize(av.getSize());

		sm.getSession(profile).createVolume(avso);

		ArcticTask<?, ?> task = sm.getSession(profile).getVolumeTasks().get(av.getName());
		if (task != null) task.setOnPersist(r -> {
			av.setProviderId(avso.getProviderId());
			av.setBuilt(true);
			volumeRepo.save(av);
		});
	}

	public void destroyHostCollection(HostCollection hc) {
		java.util.Set<String> resolvedNetNames = new java.util.HashSet<>();
		for (String netId : hc.getNetworks()) {
			networkRepo.findById(netId).ifPresentOrElse(
				n -> resolvedNetNames.add(n.getNetName()),
				() -> resolvedNetNames.add(netId)
			);
		}

		for (ArcticHost instance : hc.getInstances()) {
			ArcticHostSO ahso = new ArcticHostSO();
			ahso.setName(instance.getName());
			ahso.setIp(instance.getIp());
			ahso.setRangeId(instance.getRangeId());
			ahso.setProviderId(instance.getProviderId());
			ahso.setOsType(hc.getOsType());
			ahso.setNetworks(new java.util.HashSet<>(resolvedNetNames));
			ahso.setVolumes(new java.util.HashSet<>(hc.getVolumes()));
			ahso.setExtraVariables(new java.util.HashMap<>(hc.getExtraVariables()));
			ahso.setCollectionId(hc.getId());

			sm.getSession(profile).destroyHost(ahso);
		}
	}

	// Tears down the Ansible Controller
	public void destroyAnsibleController(RangeExercise ex) {
		if (ex == null) return;
		String providerId = ex.getControllerProviderId();
		if (providerId == null || providerId.isBlank()) {
			ARCTICLog.print("IcebergCreator", "no controllerProviderId on exercise '"
					+ ex.getName() + "' — skipping controller destroy");
			return;
		}

		String name = "arctic-controller-" + ex.getName();
		ArcticHostSO ahso = new ArcticHostSO();
		ahso.setName(name);
		ahso.setRangeId(ex.getId());
		ahso.setProviderId(providerId);
		ahso.setDestroyPriorityOverride(3);

		sm.getSession(profile).destroyHost(ahso);

		ArcticTask<?, ?> task = sm.getSession(profile).getDestroyInstanceTasks().get(name);
		if (task != null) task.setOnPersist(r -> {
			ex.setControllerProviderId(null);
			exerciseRepo.save(ex);
			ARCTICLog.print("IcebergCreator", "controller destroyed + providerId cleared for '"
					+ ex.getName() + "'");
		});
	}

	public void destroyNetwork(ArcticNetwork an) {
		ArcticNetworkSO anso = new ArcticNetworkSO();
		anso.setName(an.getNetName());
		anso.setIpCidr(an.getNetCidr());
		anso.setIpGateway(an.getGateway());
		anso.setIpRangeEnd(an.getNetEnd());
		anso.setIpRangeStart(an.getNetStart());
		anso.setRangeId(an.getRangeId());
		anso.setProviderId(an.getProviderId());
		anso.setExtraVariables(an.getExtraVariables());

		sm.getSession(profile).destroyNetwork(anso);
	}

	public void destroySecurityGroup(ArcticSecurityGroup asg) {
		ArcticSecurityGroupSO asgso = new ArcticSecurityGroupSO();
		asgso.setDescription(asg.getDescription());
		asgso.setName(asg.getName());
		asgso.setRangeId(asg.getRangeId());
		asgso.setProviderId(asg.getProviderId());

		sm.getSession(profile).destroySecurityGroup(asgso);
	}

	public void destroySecurityGroupRule(ArcticSecurityGroupRule asgr) {
		ArcticSecurityGroupRuleSO asgrso = new ArcticSecurityGroupRuleSO();
		asgrso.setDescription(asgr.getDescription());
		asgrso.setDirection(asgr.getDirection());
		asgrso.setEndPortRange(asgr.getEndPortRange());
		asgrso.setEth(asgr.getEth());
		asgrso.setName(asgr.getName());
		asgrso.setProtocol(asgr.getProtocol());
		asgrso.setRangeId(asgr.getRangeId());
		asgrso.setSecGroup(asgr.getSecGroup());
		asgrso.setStartPortRange(asgr.getStartPortRange());
		asgrso.setProviderId(asgr.getProviderId());

		sm.getSession(profile).destroySecurityGroupRule(asgrso);
	}

	public void destroyRouter(ArcticRouter ar) {
		ArcticRouterSO arso = new ArcticRouterSO();
		arso.setConnectedNetworkNames(ar.getNetworks());
		arso.setName(ar.getName());
		arso.setRangeId(ar.getRangeId());
		arso.setProviderId(ar.getProviderId());
		arso.setExtraVariables(ar.getExtraVariables());

		sm.getSession(profile).destroyRouter(arso);
	}

	public void destroyVolume(ArcticVolume av) {
		ArcticVolumeSO avso = new ArcticVolumeSO();
		avso.setBootable(av.isBootable());
		avso.setDescription(av.getDescription());
		avso.setImageId(av.getImageId());
		avso.setName(av.getName());
		avso.setRangeId(av.getRangeId());
		avso.setSize(av.getSize());
		avso.setProviderId(av.getProviderId());

		sm.getSession(profile).destroyVolume(avso);
	}

	// TODO: Needs to be fixed with user profiles and who is doing this
	public void run() {
		if (deploymentId != null) ARCTICLog.setDeployment(deploymentId);
		try {
			if (destroyMode) {
				tasksToComplete.addAll(sm.getSession(profile).getDestroyInstanceTasks().values());
				tasksToComplete.addAll(sm.getSession(profile).getDestroyNetworkTasks().values());
				tasksToComplete.addAll(sm.getSession(profile).getDestroyRouterTasks().values());
				tasksToComplete.addAll(sm.getSession(profile).getDestroySecurityGroupRuleTasks().values());
				tasksToComplete.addAll(sm.getSession(profile).getDestroySecurityGroupTasks().values());
				tasksToComplete.addAll(sm.getSession(profile).getDestroyVolumeTasks().values());
			} else {
				tasksToComplete.addAll(sm.getSession(profile).getInstanceTasks().values());
				tasksToComplete.addAll(sm.getSession(profile).getNetworkTasks().values());
				tasksToComplete.addAll(sm.getSession(profile).getRouterTasks().values());
				tasksToComplete.addAll(sm.getSession(profile).getSecurityGroupRuleTasks().values());
				tasksToComplete.addAll(sm.getSession(profile).getSecurityGroupTasks().values());
				tasksToComplete.addAll(sm.getSession(profile).getVolumeTasks().values());
			}

			PriorityQueue<ArcticTask<?, ?>> queue = new PriorityQueue<ArcticTask<?, ?>>(tasksToComplete.size(), new Comparator<ArcticTask<?, ?>>() {
				@Override
				public int compare(ArcticTask<?, ?> o1, ArcticTask<?, ?> o2) {
					return o1.getPriority() - o2.getPriority();
				}
			});
			queue.addAll(tasksToComplete);

			while(!queue.isEmpty()) {
				executorService.execute(ARCTICLog.wrap(queue.poll()));
			}

			executorService.shutdown();
			try {
				if (!executorService.awaitTermination(1, TimeUnit.HOURS)) {
					ARCTICLog.err("IcebergCreator", "task executor did not terminate within 1h");
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}

			tasksToComplete.clear();

			if (!destroyMode && exercise != null) {
				ansibleStager.stage(exercise);
				ansibleStager.pushAndRun(controllerIp, exercise.getName());
			}
		} finally {
			if (deploymentId != null) ARCTICLog.clearDeployment();
		}
	}
	
}