package com.codearena.module1_challenge.service;

import com.codearena.module1_challenge.dto.ChallengeDto;
import com.codearena.module1_challenge.dto.CreateChallengeRequest;
import com.codearena.module1_challenge.dto.TestCaseDto;
import com.codearena.module1_challenge.repository.ChallengeRepository;
import com.codearena.module1_challenge.repository.TestCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChallengeServiceImplTest {

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private TestCaseRepository testCaseRepository;

    @InjectMocks
    private ChallengeServiceImpl challengeService;

    private Object[] sampleRow;

    @BeforeEach
    void setUp() {
        sampleRow = new Object[]{
                1L,                                  // id
                "Two Sum",                           // title
                "Given an array of integers...",      // description
                "EASY",                              // difficulty
                "Array, Hash Table",                 // tags
                "Java",                              // language
                "auth0|user123",                     // authorId
                Timestamp.from(Instant.now())        // createdAt
        };
    }

    @Nested
    @DisplayName("getAllChallenges")
    class GetAllChallenges {

        @Test
        @DisplayName("should return a list of all challenges mapped to DTOs")
        void shouldReturnAllChallenges() {
            List<Object[]> rows = new ArrayList<>();
            rows.add(sampleRow);
            when(challengeRepository.findAllSanitized()).thenReturn(rows);
            when(testCaseRepository.findRawByNumericChallengeId(1L)).thenReturn(Collections.emptyList());

            List<ChallengeDto> result = challengeService.getAllChallenges();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Two Sum");
            assertThat(result.get(0).getDifficulty()).isEqualTo("EASY");
            verify(challengeRepository).findAllSanitized();
        }

        @Test
        @DisplayName("should return empty list when no challenges exist")
        void shouldReturnEmptyList() {
            when(challengeRepository.findAllSanitized()).thenReturn(Collections.emptyList());

            List<ChallengeDto> result = challengeService.getAllChallenges();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getChallengeById")
    class GetChallengeById {

        @Test
        @DisplayName("should return the correct challenge DTO for a valid ID")
        void shouldReturnChallenge() {
            List<Object[]> rows = new ArrayList<>();
            rows.add(sampleRow);
            when(challengeRepository.findByIdSanitized(1L)).thenReturn(rows);
            when(testCaseRepository.findRawByNumericChallengeId(1L)).thenReturn(Collections.emptyList());

            ChallengeDto result = challengeService.getChallengeById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getTitle()).isEqualTo("Two Sum");
            assertThat(result.getTags()).isEqualTo("Array, Hash Table");
        }

        @Test
        @DisplayName("should throw RuntimeException when challenge not found")
        void shouldThrowWhenNotFound() {
            when(challengeRepository.findByIdSanitized(999L)).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> challengeService.getChallengeById(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Challenge not found: 999");
        }
    }

    @Nested
    @DisplayName("createChallenge")
    class CreateChallenge {

        @Test
        @DisplayName("should insert a challenge and its test cases then return the DTO")
        void shouldCreateChallengeWithTestCases() {
            CreateChallengeRequest request = CreateChallengeRequest.builder()
                    .title("New Problem")
                    .description("Solve this")
                    .difficulty("MEDIUM")
                    .tags("Array")
                    .language("Python")
                    .testCases(List.of(
                            TestCaseDto.builder().input("[1,2]").expectedOutput("3").isHidden(false).build()
                    ))
                    .build();

            when(challengeRepository.findNextNumericId()).thenReturn(1_000_001L);
            when(challengeRepository.existsByNumericId(1_000_001L)).thenReturn(0);
            when(testCaseRepository.findNextNumericId()).thenReturn(1_000_001L);
            when(testCaseRepository.existsByNumericId(1_000_001L)).thenReturn(0);

            Object[] createdRow = new Object[]{
                    1_000_001L, "New Problem", "Solve this", "MEDIUM", "Array", "Python", "author1",
                    Timestamp.from(Instant.now())
            };
            List<Object[]> createdRows = new ArrayList<>();
            createdRows.add(createdRow);
            when(challengeRepository.findByIdSanitized(1_000_001L)).thenReturn(createdRows);
            when(testCaseRepository.findRawByNumericChallengeId(1_000_001L)).thenReturn(Collections.emptyList());

            ChallengeDto result = challengeService.createChallenge(request, "author1");

            assertThat(result.getTitle()).isEqualTo("New Problem");
            verify(challengeRepository).insertChallenge(eq(1_000_001L), eq("New Problem"),
                    eq("Solve this"), eq("MEDIUM"), eq("Array"), eq("Python"), eq("author1"));
            verify(testCaseRepository).insertTestCase(eq(1_000_001L), eq(1_000_001L),
                    eq("[1,2]"), eq("3"), eq(0));
        }

        @Test
        @DisplayName("should handle null test cases without error")
        void shouldHandleNullTestCases() {
            CreateChallengeRequest request = CreateChallengeRequest.builder()
                    .title("No Tests")
                    .description("Desc")
                    .difficulty("EASY")
                    .tags("Math")
                    .language("Java")
                    .testCases(null)
                    .build();

            when(challengeRepository.findNextNumericId()).thenReturn(1_000_002L);
            when(challengeRepository.existsByNumericId(1_000_002L)).thenReturn(0);

            Object[] createdRow = new Object[]{
                    1_000_002L, "No Tests", "Desc", "EASY", "Math", "Java", "author1",
                    Timestamp.from(Instant.now())
            };
            List<Object[]> createdRows = new ArrayList<>();
            createdRows.add(createdRow);
            when(challengeRepository.findByIdSanitized(1_000_002L)).thenReturn(createdRows);
            when(testCaseRepository.findRawByNumericChallengeId(1_000_002L)).thenReturn(Collections.emptyList());

            ChallengeDto result = challengeService.createChallenge(request, "author1");

            assertThat(result.getTitle()).isEqualTo("No Tests");
            verify(testCaseRepository, never()).insertTestCase(anyLong(), anyLong(), any(), any(), anyInt());
        }
    }

    @Nested
    @DisplayName("updateChallenge")
    class UpdateChallenge {

        @Test
        @DisplayName("should update a challenge and replace its test cases")
        void shouldUpdateChallenge() {
            List<Object[]> rows = new ArrayList<>();
            rows.add(sampleRow);
            when(challengeRepository.findByIdSanitized(1L)).thenReturn(rows);
            when(testCaseRepository.findRawByNumericChallengeId(1L)).thenReturn(Collections.emptyList());
            when(testCaseRepository.findNextNumericId()).thenReturn(1_000_001L);
            when(testCaseRepository.existsByNumericId(1_000_001L)).thenReturn(0);

            CreateChallengeRequest request = CreateChallengeRequest.builder()
                    .title("Updated Title")
                    .description("Updated Desc")
                    .difficulty("HARD")
                    .tags("Graph")
                    .language("C++")
                    .testCases(List.of(
                            TestCaseDto.builder().input("5").expectedOutput("10").isHidden(true).build()
                    ))
                    .build();

            ChallengeDto result = challengeService.updateChallenge(1L, request);

            verify(challengeRepository).updateChallengeByNumericId(1L, "Updated Title",
                    "Updated Desc", "HARD", "Graph", "C++");
            verify(testCaseRepository).deleteByNumericChallengeId(1L);
            verify(testCaseRepository).insertTestCase(eq(1_000_001L), eq(1L),
                    eq("5"), eq("10"), eq(1));
        }

        @Test
        @DisplayName("should throw when updating a non-existent challenge")
        void shouldThrowWhenNotFound() {
            when(challengeRepository.findByIdSanitized(999L)).thenReturn(Collections.emptyList());

            CreateChallengeRequest request = CreateChallengeRequest.builder().title("x").build();

            assertThatThrownBy(() -> challengeService.updateChallenge(999L, request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Challenge not found: 999");
        }
    }

    @Nested
    @DisplayName("deleteChallenge")
    class DeleteChallenge {

        @Test
        @DisplayName("should delegate deletion to the repository")
        void shouldDeleteChallenge() {
            challengeService.deleteChallenge(1L);
            verify(challengeRepository).deleteById(1L);
        }
    }
}
