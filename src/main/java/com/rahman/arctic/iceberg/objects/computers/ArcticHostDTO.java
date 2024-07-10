package com.rahman.arctic.iceberg.objects.computers;

import java.util.HashSet;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data-transfer object for turning JSON into an ArcticHost
 * @author SGT Rahman
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArcticHostDTO {
	private int count = 1;
	private int mapId;
	private String name;
	private int size;
	private String imageId;
	private String flavorId;
	private String imageName;
	private String flavorName;
	private Set<String> networks = new HashSet<>();
	private Set<String> volumes = new HashSet<>();
}