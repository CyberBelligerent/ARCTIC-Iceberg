package com.rahman.arctic.iceberg.objects.computers;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArcticSecurityGroupDTO {
	private String name;
	private String rangeId;
	private String description = "";
}