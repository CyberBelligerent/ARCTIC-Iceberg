package com.rahman.arctic.iceberg.objects.computers;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class ArcticSecurityGroup {
	
	@Id
	private String id;
	
	private String name;
	private String rangeId;
	private String description;
	
	public ArcticSecurityGroup() {
		id = UUID.randomUUID().toString();
	}
	
}