package com.rahman.arctic.iceberg.objects.computers;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class ArcticVolume {

	@Id
	private String id;
	
	private String name;
	private String description;
	private String imageId;
	private boolean bootable;
	private int size;
	private boolean built = false;
	private boolean errorState = false;
	private String rangeId;
	
	public ArcticVolume() {
		id = UUID.randomUUID().toString();
	}
	
}