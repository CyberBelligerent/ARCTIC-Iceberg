package com.rahman.arctic.iceberg.objects.computers;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class ArcticHost {

	@Id
	private String id;

	@Column(name = "collection_id", insertable = false, updatable = false)
	private String collectionId;
	private int instanceIndex;

	private String name;
	private String ip;
	private String providerId;

	private boolean built = false;
	private boolean errorState = false;

	private String rangeId;

	public ArcticHost() {
		id = UUID.randomUUID().toString();
	}

}
