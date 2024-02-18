package com.rahman.arctic.iceberg.repos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rahman.arctic.iceberg.objects.computers.ArcticVolume;

@Repository
public interface ArcticVolumeRepo extends JpaRepository<ArcticVolume, String>{}