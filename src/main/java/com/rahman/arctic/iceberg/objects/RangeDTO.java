package com.rahman.arctic.iceberg.objects;

import lombok.Data;

/**
 * Data-transfer object for turning JSON into a RangeExercise
 * @author SGT Rahman
 *
 */
@Data
public class RangeDTO {
	private String rangeName;
	private String rangeDescription;
	private String providerName;
//	private String rangeType;
//	private int concurrentRanges;
//	private List<String> tags;	
}