package com.codearena.module1_challenge.repository;

import com.codearena.module1_challenge.entity.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

		@Query(value = """
						SELECT COALESCE(MAX(CAST(tc.id AS UNSIGNED)), 0) + 1
						FROM test_case tc
						WHERE tc.id REGEXP '^[0-9]+$'
						""", nativeQuery = true)
		Long findNextNumericId();

		@Modifying
		@Query(value = """
						INSERT INTO test_case (id, challenge_id, input, expected_output, is_hidden)
						VALUES (:id, :challengeId, :input, :expectedOutput, :isHidden)
						""", nativeQuery = true)
		int insertTestCase(@Param("id") Long id,
					   @Param("challengeId") Long challengeId,
					   @Param("input") String input,
					   @Param("expectedOutput") String expectedOutput,
					   @Param("isHidden") Integer isHidden);

		@Query(value = """
						SELECT tc.input, tc.expected_output, COALESCE(tc.is_hidden, 0)
						FROM test_case tc
						WHERE tc.challenge_id REGEXP '^[0-9]+$'
							AND CAST(tc.challenge_id AS UNSIGNED) = :challengeId
						""", nativeQuery = true)
		List<Object[]> findRawByNumericChallengeId(@Param("challengeId") long challengeId);
}
