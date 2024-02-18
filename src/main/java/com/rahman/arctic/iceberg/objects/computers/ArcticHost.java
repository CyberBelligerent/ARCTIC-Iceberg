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
public class ArcticHost {

	@Id
	private String id;
	
	private int mapId;
	private int count = 1;
	private String name;
	private int size;
	private String imageId;
	private String flavorId;
	private boolean built = false;
	private boolean errorState = false;
	private String ip;
	private String rangeId;
	
	@ElementCollection
	private Set<String> networks = new HashSet<>();
	
	@ElementCollection
	private Set<String> volumes = new HashSet<>();
	
	public ArcticHost() {
		id = UUID.randomUUID().toString();
	}
	
}