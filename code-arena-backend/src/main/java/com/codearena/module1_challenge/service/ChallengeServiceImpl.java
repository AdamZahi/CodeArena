package com.codearena.module1_challenge.service;

import com.codearena.module1_challenge.dto.ChallengeDto;
import com.codearena.module1_challenge.dto.CreateChallengeRequest;
import com.codearena.module1_challenge.dto.TestCaseDto;
import com.codearena.module1_challenge.entity.Challenge;
import com.codearena.module1_challenge.entity.TestCase;
import com.codearena.module1_challenge.repository.ChallengeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChallengeServiceImpl implements ChallengeService {

    private final ChallengeRepository challengeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ChallengeDto> getAllChallenges() {
        return challengeRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ChallengeDto getChallengeById(Long id) {
        return challengeRepository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new RuntimeException("Challenge not found: " + id));
    }

    @Override
    @Transactional
    public ChallengeDto createChallenge(CreateChallengeRequest request, String authorId) {
        Challenge challenge = Challenge.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .difficulty(request.getDifficulty())
                .tags(request.getTags())
                .language(request.getLanguage())
                .authorId(authorId)
                .createdAt(Instant.now())
                .testCases(new ArrayList<>())
                .build();

        if (request.getTestCases() != null) {
            for (TestCaseDto tcDto : request.getTestCases()) {
                TestCase tc = TestCase.builder()
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

    @Override
    public void deleteChallenge(Long id) {
        challengeRepository.deleteById(id);
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
}
