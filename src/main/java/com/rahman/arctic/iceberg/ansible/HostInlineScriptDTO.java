package com.rahman.arctic.iceberg.ansible;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HostInlineScriptDTO {
	private String name;
	private int runOrder;
	private String content;
}
