package com.rahman.arctic.iceberg.objects.computers;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class ArcticSecurityGroupRule {

	@Id
	private String id;
	
	private String name;
	private String description;
	private String secGroup;
	private String direction;
	private int startPortRange;
	private int endPortRange;
	private String protocol;
	private String eth;
	private String rangeId;
	
	public ArcticSecurityGroupRule() {
		id = UUID.randomUUID().toString();
	}
	
}