package com.rahman.arctic.iceberg.objects;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class RangeAgent {

	@Id
	@Column(name = "agent_id")
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

	@Column(nullable = false)
	private String tokenHash;

	@Column(name = "exercise_id")
	private String exerciseId;

	@Column(name = "host_id")
	private String hostId;

	private String hostname;

	private Instant createdAt;
	private Instant lastSeen;

}
