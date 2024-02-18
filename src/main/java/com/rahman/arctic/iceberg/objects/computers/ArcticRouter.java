package com.rahman.arctic.iceberg.objects.computers;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class ArcticRouter {

	@Id
	private String id;
	
	private int mapId;
	private String name;
	private boolean built = false;
	private boolean errorState = false;
	private String rangeId;
	
	@ElementCollection
	private Set<String> networks = new HashSet<>();
	
	public ArcticRouter() {
		id = UUID.randomUUID().toString();
	}
	
}