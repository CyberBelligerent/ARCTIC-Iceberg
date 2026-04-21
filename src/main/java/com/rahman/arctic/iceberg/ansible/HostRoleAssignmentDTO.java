package com.rahman.arctic.iceberg.ansible;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HostRoleAssignmentDTO {
	private String roleId;
	private int runOrder;
	private Map<String, String> overrideVariables = new HashMap<>();
}
