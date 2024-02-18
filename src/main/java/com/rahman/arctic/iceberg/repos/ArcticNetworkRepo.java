package com.rahman.arctic.iceberg.repos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rahman.arctic.iceberg.objects.computers.ArcticNetwork;

@Repository
public interface ArcticNetworkRepo extends JpaRepository<ArcticNetwork, String>{}