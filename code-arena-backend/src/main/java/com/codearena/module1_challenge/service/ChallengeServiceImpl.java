package com.codearena.module1_challenge.service;

import com.codearena.module1_challenge.dto.ChallengeDto;
import com.codearena.module1_challenge.dto.CreateChallengeRequest;
import com.codearena.module1_challenge.dto.TestCaseDto;
import com.codearena.module1_challenge.entity.Challenge;
import com.codearena.module1_challenge.entity.TestCase;
import com.codearena.module1_challenge.repository.ChallengeRepository;
import com.codearena.module1_challenge.repository.TestCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
        Long nextId = challengeRepository.findNextNumericId();
        if (nextId == null || nextId < 1) {
            nextId = 1L;
        }
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
        Challenge challenge = challengeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Challenge not found: " + id));
        long nextTestCaseId = nextTestCaseId();

        challenge.setTitle(request.getTitle());
        challenge.setDescription(request.getDescription());
        challenge.setDifficulty(request.getDifficulty());
        challenge.setTags(request.getTags());
        challenge.setLanguage(request.getLanguage());

        challenge.getTestCases().clear();
        if (request.getTestCases() != null) {
            for (TestCaseDto tcDto : request.getTestCases()) {
                TestCase tc = TestCase.builder()
                        .id(nextTestCaseId++)
                        .input(tcDto.getInput())
                        .expectedOutput(tcDto.getExpectedOutput())
                        .isHidden(tcDto.getIsHidden())
                        .challenge(challenge)
                        .build();
                challenge.getTestCases().add(tc);
            }
        }

        challenge = challengeRepository.save(challenge);
        return mapToDto(challenge);
    }

    private ChallengeDto mapToDto(Challenge challenge) {
        return ChallengeDto.builder()
                .id(challenge.getId())
                .title(challenge.getTitle())
                .description(challenge.getDescription())
                .difficulty(challenge.getDifficulty())
                .tags(challenge.getTags())
                .language(challenge.getLanguage())
                .authorId(challenge.getAuthorId())
                .createdAt(challenge.getCreatedAt())
                .testCases(challenge.getTestCases().stream()
                        .map(tc -> TestCaseDto.builder()
                                .id(tc.getId())
                                .input(tc.getInput())
                                .expectedOutput(tc.getExpectedOutput())
                                .isHidden(tc.getIsHidden())
                                .build())
                        .collect(Collectors.toList()))
                .build();
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
        if (nextId == null || nextId < 1) {
            return 1L;
        }
        return nextId;
    }
}
