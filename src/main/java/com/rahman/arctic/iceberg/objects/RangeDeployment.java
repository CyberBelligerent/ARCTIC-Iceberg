package com.rahman.arctic.iceberg.objects;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;

/**
 * Represents a live deployment of a RangeExercise template against a hypervisor.
 * One template can have many deployments.
 */
@Entity
@Data
public class RangeDeployment {

	@Id
	@Column(name = "deployment_id")
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

	private String exerciseId;
	private String exerciseName;
	private String domain;
	private String profileName;
	private String deployedBy;

	@Temporal(TemporalType.TIMESTAMP)
	private Date deployedAt;
	private String status;

}
