package com.rahman.arctic.iceberg.ansible;

import java.util.HashSet;
import java.util.Set;

import lombok.Data;

@Data
public class HostAnsibleViewDTO {
	private Set<HostRoleAssignment> roleAssignments = new HashSet<>();
	private Set<HostInlineScript> inlineScripts = new HashSet<>();
}
