package com.rahman.arctic.iceberg.objects.computers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HostCollectionDTO {
	private String name;
	private int count = 1;
	private int mapId;
	private String osType;
	private Set<String> networks = new HashSet<>();
	private Set<String> volumes = new HashSet<>();
	private Map<String, String> extraVariables = new HashMap<>();
}
