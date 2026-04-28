package com.rahman.arctic.iceberg.objects.computers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.rahman.arctic.iceberg.ansible.HostInlineScript;
import com.rahman.arctic.iceberg.ansible.HostRoleAssignment;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import lombok.Data;

@Entity
@Data
public class HostCollection {

	@Id
	private String id;

	private String name;
	private int count = 1;
	private int mapId;
	private String osType;
	private String rangeId;

	@ElementCollection
	private Set<String> networks = new HashSet<>();

	@ElementCollection
	private Set<String> volumes = new HashSet<>();

	@ElementCollection
	@Column(length = 4096)
	private Map<String, String> extraVariables = new HashMap<>();

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	@JoinColumn(name = "collection_id")
	private Set<HostRoleAssignment> roleAssignments = new HashSet<>();

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	@JoinColumn(name = "collection_id")
	private Set<HostInlineScript> inlineScripts = new HashSet<>();

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	@JoinColumn(name = "collection_id")
	private Set<ArcticHost> instances = new HashSet<>();

	public HostCollection() {
		id = UUID.randomUUID().toString();
	}

}
