package com.rahman.arctic.iceberg.services;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.rahman.arctic.iceberg.objects.RangeAgent;
import com.rahman.arctic.iceberg.repos.RangeAgentRepo;

@Service
public class RangeAgentLookupService {

	private final RangeAgentRepo agentRepo;

	public RangeAgentLookupService(RangeAgentRepo agentRepo) {
		this.agentRepo = agentRepo;
	}

	public RangeAgent create(String exerciseId, String hostId, String hostname, String tokenHash) {
		RangeAgent agent = new RangeAgent();
		agent.setExerciseId(exerciseId);
		agent.setHostId(hostId);
		agent.setHostname(hostname);
		agent.setTokenHash(tokenHash);
		agent.setCreatedAt(Instant.now());
		return agentRepo.save(agent);
	}

	public Optional<RangeAgent> findById(String id) {
		return agentRepo.findById(id);
	}

	public List<RangeAgent> findByExerciseId(String exerciseId) {
		return agentRepo.findByExerciseId(exerciseId);
	}

	public List<RangeAgent> findByHostId(String hostId) {
		return agentRepo.findByHostId(hostId);
	}

	public void touchLastSeen(String agentId) {
		agentRepo.findById(agentId).ifPresent(a -> {
			a.setLastSeen(Instant.now());
			agentRepo.save(a);
		});
	}

	public void delete(String agentId) {
		agentRepo.deleteById(agentId);
	}

}
