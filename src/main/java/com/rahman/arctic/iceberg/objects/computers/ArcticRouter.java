package com.rahman.arctic.iceberg.objects.computers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
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
	private String providerId;
	
	@ElementCollection
	private Set<String> networks = new HashSet<>();

	@ElementCollection
	@CollectionTable(name = "arctic_router_extra_variables", joinColumns = @JoinColumn(name = "router_id"))
	@MapKeyColumn(name = "var_key")
	private Map<String, String> extraVariables = new HashMap<>();
	
	public ArcticRouter() {
		id = UUID.randomUUID().toString();
	}
	
}