package com.rahman.arctic.iceberg.objects.computers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
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
	private String providerId;

	@ElementCollection
	@CollectionTable(name = "arctic_network_extra_variables", joinColumns = @JoinColumn(name = "network_id"))
	@MapKeyColumn(name = "var_key")
	@Column(length = 4096)
	private Map<String, String> extraVariables = new HashMap<>();
	
	public ArcticNetwork() {
		id = UUID.randomUUID().toString();
	}
	
}