package com.rahman.arctic.iceberg.objects;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.rahman.arctic.iceberg.objects.computers.ArcticHost;
import com.rahman.arctic.iceberg.objects.computers.ArcticNetwork;
import com.rahman.arctic.iceberg.objects.computers.ArcticRouter;
import com.rahman.arctic.iceberg.objects.computers.ArcticVolume;
import com.rahman.arctic.polarbear.objects.AttackStepRef;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Stores network information, reports, and teams to help gamify the exercise/ranges
 * @author SGT Rahman
 *
 */
@Entity
@Data
@EqualsAndHashCode(exclude = { /*"teams",*/"networks", "hosts", "volumes", "attackSteps"})
public class RangeExercise {
	
	@Id
	@Column(name = "exercise_id")
	private String id;
	
	@Lob
	private String description;
	
	private String name;
	private String projectId;
	private RangeType type;
	
	private int concurrentRanges;
	
	/**
	 * All teams assigned to this Range
	 */
//	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
//	@JoinColumn(name = "exercise_id")
//	private Set<RangeTeam> teams = new HashSet<>();
	
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	@JoinColumn(name = "exercise_id")
	private Set<ArcticNetwork> networks = new HashSet<>();
	
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	@JoinColumn(name = "exercise_id")
	private Set<ArcticHost> hosts = new HashSet<>();
	
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	@JoinColumn(name = "exercise_id")
	private Set<ArcticVolume> volumes = new HashSet<>();
	
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	@JoinColumn(name = "exercise_id")
	private Set<ArcticRouter> routers = new HashSet<>();
	
	@ElementCollection
	private Set<AttackStepRef> attackSteps = new HashSet<>();
	
	@ElementCollection
	private Set<String> tags = new HashSet<>();
	
	public RangeExercise() {
		id = UUID.randomUUID().toString();
	}
	
	public boolean doesAttackStepsContainId(String id) {
		AttackStepRef asr = attackSteps.stream().filter((e) -> e.getAttackStepId() == id).findFirst().orElse(null);
		return (asr != null);
	}
	
}