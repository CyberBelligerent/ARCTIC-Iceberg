package com.rahman.arctic.iceberg.objects.computers;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

/**
 * Actual implementation of the Network object for the range exercise
 * @author SGT Rahman
 *
 */
@Entity
@Data
public class ArcticNetwork {

	@Id
	private String id;
	
	private int mapId;
	private String netName;
	private String netCidr;
	private String netStart;
	private String netEnd;
	private String gateway;
	private boolean built = false;
	private boolean errorState = false;
	private String rangeId;
	
	public ArcticNetwork() {
		id = UUID.randomUUID().toString();
	}
	
}