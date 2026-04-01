package com.codearena.module1_challenge.repository;

import com.codearena.module1_challenge.entity.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    // Step 5: daily challenge generation
    List<Challenge> findByDifficulty(String difficulty);

        // Legacy DB safety: read only rows with numeric IDs and cast explicitly.
        @Query(value = """
            SELECT CAST(c.id AS UNSIGNED) AS id,
               c.title,
               c.description,
               c.difficulty,
               c.tags,
               c.language,
               c.author_id,
               c.created_at
            FROM challenge c
            WHERE c.id REGEXP '^[0-9]+$'
            """, nativeQuery = true)
        List<Object[]> findAllSanitized();

        @Query(value = """
            SELECT CAST(c.id AS UNSIGNED) AS id,
               c.title,
               c.description,
               c.difficulty,
               c.tags,
               c.language,
               c.author_id,
               c.created_at
            FROM challenge c
            WHERE c.id REGEXP '^[0-9]+$'
              AND LOWER(c.difficulty) = LOWER(:difficulty)
            """, nativeQuery = true)
        List<Object[]> findByDifficultySanitized(@Param("difficulty") String difficulty);
}
