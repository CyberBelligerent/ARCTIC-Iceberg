package com.rahman.arctic.iceberg.repos;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rahman.arctic.iceberg.objects.RangeExercise;

/**
 * Spring Boot Repository (Database) management for RangeExercises
 * @author SGT Rahman
 *
 */
public interface ExerciseRepo extends JpaRepository<RangeExercise, String> {
	Optional<RangeExercise> findByName(String name);
}