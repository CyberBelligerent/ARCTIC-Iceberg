package com.rahman.arctic.iceberg.objects.computers;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data-transfer object for turning JSON into an ArcticVolume
 * @author SGT Rahman
 *
 */
@Data
@NoArgsConstructor
public class ArcticVolumeDTO {
	private String name;
	private String description;
	private String imageId;
	private int size;
}