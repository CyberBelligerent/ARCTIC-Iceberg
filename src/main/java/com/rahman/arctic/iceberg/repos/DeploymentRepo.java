package com.rahman.arctic.iceberg.repos;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rahman.arctic.iceberg.objects.RangeDeployment;

public interface DeploymentRepo extends JpaRepository<RangeDeployment, String> {

	List<RangeDeployment> findAllByExerciseId(String exerciseId);

	List<RangeDeployment> findAllByDeployedBy(String deployedBy);

}
