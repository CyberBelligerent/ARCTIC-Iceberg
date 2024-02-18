package com.rahman.arctic.iceberg.objects.computers;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data-transfer object for turning JSON into a set of instructions to send to OpenStack
 * @author SGT Rahman
 *
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown=true)
public class RangeCreationDTO {
	private List<ArcticNetworkDTO> networks = new ArrayList<>();
	private List<ArcticHostDTO> hosts = new ArrayList<>();
	private List<ArcticVolumeDTO> volumes = new ArrayList<>();
}