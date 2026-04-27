package com.rahman.arctic.iceberg.ansible;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class HostRoleAssignment {

	@Id
	private String id = UUID.randomUUID().toString();

	private String roleId;

	private int runOrder;

	@ElementCollection
	private Map<String, String> overrideVariables = new HashMap<>();

}
