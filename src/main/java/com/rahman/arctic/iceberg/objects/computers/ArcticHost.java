package com.rahman.arctic.iceberg.objects.computers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.rahman.arctic.iceberg.ansible.HostInlineScript;
import com.rahman.arctic.iceberg.ansible.HostRoleAssignment;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import lombok.Data;

@Entity
@Data
public class ArcticHost {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;
	
	private int mapId;
	private int count = 1;
	private String name;
//	private String defaultUser;
//	private String defaultPassword;
//	private String imageName;
//	private String imageId;
//	private String flavorName;
//	private String flavorId;
	private String osType;
	private boolean built = false;
	private boolean errorState = false;
	private String ip;
	private String rangeId;
	private String providerId;
	
//	@ElementCollection
//	private Set<String> wantedIPs = new HashSet<>();
	
	@ElementCollection
	private Set<String> networks = new HashSet<>();
	
	@ElementCollection
	private Set<String> volumes = new HashSet<>();
	
	@ElementCollection
	@Column(length = 4096)
	private Map<String, String> extraVariables = new HashMap<>();

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	@JoinColumn(name = "host_id")
	private Set<HostRoleAssignment> roleAssignments = new HashSet<>();

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	@JoinColumn(name = "host_id")
	private Set<HostInlineScript> inlineScripts = new HashSet<>();

}