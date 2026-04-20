package com.codearena.module1_challenge.service;

import com.codearena.module1_challenge.dto.ChallengeDto;
import com.codearena.module1_challenge.dto.CreateChallengeRequest;
import com.codearena.module1_challenge.dto.TestCaseDto;
import com.codearena.module1_challenge.repository.ChallengeRepository;
import com.codearena.module1_challenge.repository.TestCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChallengeServiceImpl implements ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final TestCaseRepository testCaseRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ChallengeDto> getAllChallenges() {
            return challengeRepository.findAllSanitized().stream()
                .map(this::mapSanitizedRowToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ChallengeDto getChallengeById(Long id) {
        return challengeRepository.findByIdSanitized(id).stream()
            .findFirst()
            .map(this::mapSanitizedRowToDto)
            .orElseThrow(() -> new RuntimeException("Challenge not found: " + id));
    }

    @Override
    @Transactional
    public ChallengeDto createChallenge(CreateChallengeRequest request, String authorId) {
        Long nextId = nextChallengeId();
        long nextTestCaseId = nextTestCaseId();

        challengeRepository.insertChallenge(
            nextId,
            request.getTitle(),
            request.getDescription(),
            request.getDifficulty(),
            request.getTags(),
            request.getLanguage(),
            authorId
        );

        if (request.getTestCases() != null) {
            for (TestCaseDto tcDto : request.getTestCases()) {
            testCaseRepository.insertTestCase(
                nextTestCaseId++,
                nextId,
                tcDto.getInput(),
                tcDto.getExpectedOutput(),
                Boolean.TRUE.equals(tcDto.getIsHidden()) ? 1 : 0
            );
            }
        }

        return getChallengeById(nextId);
    }

    @Override
    public void deleteChallenge(Long id) {
        challengeRepository.deleteById(id);
    }

    @Override
    @Transactional
    public ChallengeDto updateChallenge(Long id, CreateChallengeRequest request) {
        boolean exists = !challengeRepository.findByIdSanitized(id).isEmpty();
        if (!exists) {
            throw new RuntimeException("Challenge not found: " + id);
        }

        challengeRepository.updateChallengeByNumericId(
                id,
                request.getTitle(),
                request.getDescription(),
                request.getDifficulty(),
                request.getTags(),
                request.getLanguage());

        testCaseRepository.deleteByNumericChallengeId(id);

        long nextTestCaseId = nextTestCaseId();
        if (request.getTestCases() != null) {
            for (TestCaseDto tcDto : request.getTestCases()) {
                testCaseRepository.insertTestCase(
                        nextTestCaseId++,
                        id,
                        tcDto.getInput(),
                        tcDto.getExpectedOutput(),
                        Boolean.TRUE.equals(tcDto.getIsHidden()) ? 1 : 0
                );
            }
        }

        return getChallengeById(id);
    }

    private ChallengeDto mapSanitizedRowToDto(Object[] row) {
        long challengeId = asLong(row[0]);
        return ChallengeDto.builder()
                .id(challengeId)
                .title(asString(row[1]))
                .description(asString(row[2]))
                .difficulty(asString(row[3]))
                .tags(asString(row[4]))
                .language(asString(row[5]))
                .authorId(asString(row[6]))
                .createdAt(asInstant(row[7]))
                .testCases(loadSanitizedTestCases(challengeId))
                .build();
    }

    private List<TestCaseDto> loadSanitizedTestCases(long challengeId) {
        return testCaseRepository.findRawByNumericChallengeId(challengeId).stream()
                .map(tc -> TestCaseDto.builder()
                        .id(null)
                        .input(asString(tc[0]))
                        .expectedOutput(asString(tc[1]))
                        .isHidden(asBoolean(tc[2]))
                        .build())
                .toList();
    }

    private String asString(Object raw) {
        return raw == null ? null : raw.toString();
    }

    private long asLong(Object raw) {
        if (raw instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(raw.toString());
    }

    private boolean asBoolean(Object raw) {
        if (raw == null) {
            return false;
        }
        if (raw instanceof Boolean value) {
            return value;
        }
        if (raw instanceof Number number) {
            return number.intValue() != 0;
        }
        if (raw instanceof byte[] bytes && bytes.length > 0) {
            return bytes[0] != 0;
        }
        String text = raw.toString();
        return "1".equals(text) || "true".equalsIgnoreCase(text);
    }

    private Instant asInstant(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Instant instant) {
            return instant;
        }
        if (raw instanceof java.sql.Timestamp ts) {
            return ts.toInstant();
        }
        if (raw instanceof LocalDateTime ldt) {
            return ldt.toInstant(ZoneOffset.UTC);
        }
        return Instant.parse(raw.toString());
    }

    private long nextTestCaseId() {
        Long nextId = testCaseRepository.findNextNumericId();
        long candidate = (nextId == null || nextId < 1) ? System.currentTimeMillis() : nextId;

        // Legacy dumps may contain mixed ID formats; avoid low repeated IDs like 1.
        if (candidate < 1_000_000L) {
            candidate = System.currentTimeMillis();
        }

        while (existsTestCaseWithNumericId(candidate)) {
            candidate++;
        }
        return candidate;
    }

    private long nextChallengeId() {
        Long nextId = challengeRepository.findNextNumericId();
        long candidate = (nextId == null || nextId < 1) ? System.currentTimeMillis() : nextId;

        if (candidate < 1_000_000L) {
            candidate = System.currentTimeMillis();
        }

        while (existsChallengeWithNumericId(candidate)) {
            candidate++;
        }
        return candidate;
    }

    private boolean existsTestCaseWithNumericId(long id) {
        Integer count = testCaseRepository.existsByNumericId(id);
        return count != null && count > 0;
    }

    private boolean existsChallengeWithNumericId(long id) {
        Integer count = challengeRepository.existsByNumericId(id);
        return count != null && count > 0;
    }
}
