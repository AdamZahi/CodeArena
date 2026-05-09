package com.codearena.module8_terminalquest.service;

import com.codearena.module8_terminalquest.dto.CreateStoryMissionRequest;
import com.codearena.module8_terminalquest.dto.StoryMissionDto;
import com.codearena.module8_terminalquest.entity.StoryChapter;
import com.codearena.module8_terminalquest.entity.StoryMission;
import com.codearena.module8_terminalquest.repository.StoryChapterRepository;
import com.codearena.module8_terminalquest.repository.StoryMissionRepository;
import com.codearena.module8_terminalquest.tts.SpeakerRotation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoryMissionServiceImpl implements StoryMissionService {

    private final StoryMissionRepository storyMissionRepository;
    private final StoryChapterRepository storyChapterRepository;

    @Override
    @Transactional(readOnly = true)
    public List<StoryMissionDto> getMissionsByChapter(UUID chapterId) {
        return storyMissionRepository.findByChapterIdOrderByOrderIndexAsc(chapterId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public StoryMissionDto getMissionById(UUID id) {
        StoryMission mission = storyMissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Mission not found: " + id));
        return toDto(mission);
    }

    @Override
    @Transactional
    public StoryMissionDto createMission(CreateStoryMissionRequest request) {
        StoryChapter chapter = storyChapterRepository.findById(request.getChapterId())
                .orElseThrow(() -> new RuntimeException("Chapter not found: " + request.getChapterId()));
        StoryMission mission = StoryMission.builder()
                .chapter(chapter)
                .title(request.getTitle())
                .context(request.getContext())
                .task(request.getTask())
                .acceptedAnswers(request.getAcceptedAnswers())
                .hint(request.getHint())
                .orderIndex(request.getOrderIndex())
                .difficulty(request.getDifficulty())
                .isBoss(request.isBoss())
                .xpReward(request.getXpReward())
                .build();
        return toDto(storyMissionRepository.save(mission));
    }

    @Override
    @Transactional
    public StoryMissionDto updateMission(UUID id, CreateStoryMissionRequest request) {
        StoryMission mission = storyMissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mission not found: " + id));
        if (request.getChapterId() != null) {
            StoryChapter chapter = storyChapterRepository.findById(request.getChapterId())
                    .orElseThrow(() -> new RuntimeException("Chapter not found: " + request.getChapterId()));
            mission.setChapter(chapter);
        }
        mission.setTitle(request.getTitle());
        mission.setContext(request.getContext());
        mission.setTask(request.getTask());
        if (request.getAcceptedAnswers() != null && !request.getAcceptedAnswers().isBlank()) {
            mission.setAcceptedAnswers(request.getAcceptedAnswers());
        }
        mission.setHint(request.getHint());
        mission.setOrderIndex(request.getOrderIndex());
        mission.setDifficulty(request.getDifficulty());
        mission.setBoss(request.isBoss());
        mission.setXpReward(request.getXpReward());
        return toDto(storyMissionRepository.save(mission));
    }

    @Override
    @Transactional
    public void deleteMission(UUID id) {
        storyMissionRepository.deleteById(id);
    }

    StoryMissionDto toDto(StoryMission mission) {
        StoryChapter chapter = mission.getChapter();
        // Boss missions always speak with the dedicated boss speaker
        String speakerName  = mission.isBoss()
                ? SpeakerRotation.getBossSpeaker().name()
                : chapter.getSpeakerName();
        String speakerVoice = mission.isBoss()
                ? SpeakerRotation.getBossSpeaker().voice()
                : chapter.getSpeakerVoice();

        return StoryMissionDto.builder()
                .id(mission.getId())
                .chapterId(chapter.getId())
                .title(mission.getTitle())
                .context(mission.getContext())
                .task(mission.getTask())
                .hint(mission.getHint())
                .orderIndex(mission.getOrderIndex())
                .difficulty(mission.getDifficulty())
                .isBoss(mission.isBoss())
                .xpReward(mission.getXpReward())
                .speakerName(speakerName)
                .speakerVoice(speakerVoice)
                .createdAt(mission.getCreatedAt())
                .build();
    }
}
