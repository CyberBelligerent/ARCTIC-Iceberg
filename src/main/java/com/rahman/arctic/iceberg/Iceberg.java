package com.rahman.arctic.iceberg;

import org.springframework.stereotype.Service;

import com.rahman.arctic.polarbear.Polarbear;
import com.rahman.arctic.shard.Shard;

import lombok.Getter;

@Service
public class Iceberg {

	@Getter
	private Shard shard;
	
	@Getter
	private Polarbear polarbear;
	
	public Iceberg() {
		shard = new Shard();
		polarbear = new Polarbear();
		System.out.println("Enabling Service: Iceberg");
	}
	
}