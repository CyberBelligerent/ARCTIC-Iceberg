package com.rahman.arctic.iceberg.ansible;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class HostRoleAssignment {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

	private String roleId;

	private int runOrder;

	@ElementCollection
	private Map<String, String> overrideVariables = new HashMap<>();

}
