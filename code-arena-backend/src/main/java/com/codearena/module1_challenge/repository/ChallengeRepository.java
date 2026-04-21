package com.codearena.module1_challenge.repository;

import com.codearena.module1_challenge.entity.Challenge;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    @Modifying
    @Query(value = """
        INSERT INTO challenge (id, title, description, difficulty, tags, language, author_id, created_at)
        VALUES (:id, :title, :description, :difficulty, :tags, :language, :authorId, NOW(6))
        """, nativeQuery = true)
    int insertChallenge(@Param("id") Long id,
            @Param("title") String title,
            @Param("description") String description,
            @Param("difficulty") String difficulty,
            @Param("tags") String tags,
            @Param("language") String language,
            @Param("authorId") String authorId);

    @Query(value = """
            SELECT COALESCE(MAX(CAST(c.id AS UNSIGNED)), 0) + 1
            FROM challenge c
            WHERE TRIM(c.id) REGEXP '^[0-9]+$'
            """, nativeQuery = true)
    Long findNextNumericId();

        @Query(value = """
            SELECT CASE WHEN COUNT(*) > 0 THEN TRUE ELSE FALSE END
            FROM challenge c
            WHERE TRIM(c.id) REGEXP '^[0-9]+$'
              AND CAST(TRIM(c.id) AS UNSIGNED) = :id
            """, nativeQuery = true)
        int existsByNumericId(@Param("id") Long id);

        @Modifying
        @Query(value = """
            UPDATE challenge
            SET title = :title,
            description = :description,
            difficulty = :difficulty,
            tags = :tags,
            language = :language
            WHERE TRIM(id) REGEXP '^[0-9]+$'
              AND CAST(TRIM(id) AS UNSIGNED) = :id
            """, nativeQuery = true)
        int updateChallengeByNumericId(@Param("id") Long id,
            @Param("title") String title,
            @Param("description") String description,
            @Param("difficulty") String difficulty,
            @Param("tags") String tags,
            @Param("language") String language);

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
            WHERE TRIM(c.id) REGEXP '^[0-9]+$'
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
                        WHERE TRIM(c.id) REGEXP '^[0-9]+$'
              AND LOWER(c.difficulty) = LOWER(:difficulty)
            """, nativeQuery = true)
        List<Object[]> findByDifficultySanitized(@Param("difficulty") String difficulty);

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
                        WHERE TRIM(c.id) REGEXP '^[0-9]+$'
                            AND CAST(TRIM(c.id) AS UNSIGNED) = :id
                        LIMIT 1
                        """, nativeQuery = true)
                List<Object[]> findByIdSanitized(@Param("id") Long id);
}
