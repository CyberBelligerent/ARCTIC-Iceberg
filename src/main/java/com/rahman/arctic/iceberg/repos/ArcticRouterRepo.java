package com.rahman.arctic.iceberg.repos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rahman.arctic.iceberg.objects.computers.ArcticRouter;

@Repository
public interface ArcticRouterRepo extends JpaRepository<ArcticRouter, String>{}