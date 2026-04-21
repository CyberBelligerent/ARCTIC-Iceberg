package com.rahman.arctic.iceberg.ansible;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnsibleRoleDTO {
	private String name;
	private String description;
	private String content;
}
