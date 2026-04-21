package com.rahman.arctic.iceberg.repos;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rahman.arctic.iceberg.ansible.AnsibleRole;

@Repository
public interface AnsibleRoleRepo extends JpaRepository<AnsibleRole, String> {
	Optional<AnsibleRole> findByName(String name);
}
