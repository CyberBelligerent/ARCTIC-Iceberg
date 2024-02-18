package com.rahman.arctic.iceberg.objects.computers;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArcticRouterDTO {

	private String name;
	private int mapId;
	private Set<String> networks;
	
}