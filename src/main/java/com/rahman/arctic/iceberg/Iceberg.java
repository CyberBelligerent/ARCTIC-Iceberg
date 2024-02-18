package com.rahman.arctic.iceberg;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Service;

import com.rahman.arctic.polarbear.Polarbear;
import com.rahman.arctic.shard.Shard;

import lombok.Getter;

@Service
@EnableJpaRepositories(basePackages = {"com.rahman.arctic.iceberg.repos", "com.rahman.arctic.polarbear.repos", "com.rahman.arctic.shard.repos"})
@EntityScan(basePackages = {"com.rahman.arctic.iceberg.objects", "com.rahman.arctic.polarbear.objects", "com.rahman.arctic.shard.objects"})
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