package com.rahman.arctic.iceberg.objects.computers;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArcticSecurityGroupRuleDTO {
	private String name;
	private String description = "";
	private String secGroup;
	private String direction;
	private int startPortRange;
	private int endPortRange;
	private String protocol;
	private String eth;
	private String rangeId;
}