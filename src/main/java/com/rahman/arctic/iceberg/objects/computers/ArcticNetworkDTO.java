package com.rahman.arctic.iceberg.objects.computers;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data-transfer object for turning JSON into an ArcticNetwork
 * @author SGT Rahman
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArcticNetworkDTO {
	private String name;
	private int mapId;
	private String start;
	private String end;
	private String cidr;
	private String gateway;
}