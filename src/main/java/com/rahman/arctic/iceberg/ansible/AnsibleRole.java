package com.rahman.arctic.iceberg.ansible;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.Data;

@Entity
@Data
public class AnsibleRole {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

	@Column(unique = true)
	private String name;

	@Lob
	private String description;

	@Lob
	@Column(columnDefinition = "LONGTEXT")
	private String content;

}
