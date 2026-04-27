package com.rahman.arctic.iceberg.ansible;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.Data;

@Entity
@Data
public class HostInlineScript {

	@Id
	private String id = UUID.randomUUID().toString();

	private String name;

	private int runOrder;

	@Lob
	@Column(columnDefinition = "LONGTEXT")
	private String content;

}
