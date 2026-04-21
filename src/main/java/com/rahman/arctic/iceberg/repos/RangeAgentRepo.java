package com.rahman.arctic.iceberg.repos;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rahman.arctic.iceberg.objects.RangeAgent;

@Repository
public interface RangeAgentRepo extends JpaRepository<RangeAgent, String> {

	List<RangeAgent> findByExerciseId(String exerciseId);

	List<RangeAgent> findByHostId(String hostId);

}
