package com.codearena.module6_event.service;

import com.codearena.module6_event.dto.EventDto;
import com.codearena.module6_event.entity.EventRegistration;
import com.codearena.module6_event.entity.ProgrammingEvent;
import com.codearena.module6_event.mapper.EventMapper;
import com.codearena.module6_event.repository.EventRegistrationRepository;
import com.codearena.module6_event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventRecommendationService {

    private static final int RECOMMENDED_LIMIT = 3;

    private final OllamaChatModel chatModel;
    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final EventMapper eventMapper;

    @Transactional(readOnly = true)
    public List<EventDto> getRecommendedEvents(String participantId) {
        List<EventRegistration> history = registrationRepository.findByParticipantId(participantId);

        List<ProgrammingEvent> upcomingEvents = eventRepository.findAll().stream()
                .filter(event -> event.getStartDate() != null && event.getStartDate().isAfter(LocalDateTime.now()))
                .toList();

        if (upcomingEvents.isEmpty()) {
            return List.of();
        }

        String historyText = history.stream()
                .map(registration -> registration.getEvent().getTitle() + " (" + registration.getEvent().getCategory() + ")")
                .collect(Collectors.joining(", "));

        String eventsText = upcomingEvents.stream()
                .map(event -> event.getId() + ": " + event.getTitle() + " (" + event.getCategory() + ") - " + event.getType())
                .collect(Collectors.joining("\n"));

        String prompt = """
                You are an event recommendation system for CodeArena,
                a competitive programming platform.

                Player's event history: %s

                Available upcoming events:
                %s

                Based on the player's history, recommend the TOP 3
                most suitable events.

                Reply ONLY with the event IDs separated by commas.
                Example: uuid1,uuid2,uuid3
                """.formatted(historyText.isEmpty() ? "No history yet" : historyText, eventsText);

        try {
            String response = chatModel.call(prompt).trim();
            log.info("AI recommendation response: {}", response);

            List<UUID> recommendedIds = Arrays.stream(response.split(","))
                    .map(String::trim)
                    .map(this::toUuidOrNull)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new))
                    .stream()
                    .limit(RECOMMENDED_LIMIT)
                    .toList();

            if (recommendedIds.isEmpty()) {
                return fallbackRecommendations(upcomingEvents);
            }

            List<EventDto> recommendedEvents = upcomingEvents.stream()
                    .filter(event -> recommendedIds.contains(event.getId()))
                    .sorted((a, b) -> Integer.compare(recommendedIds.indexOf(a.getId()), recommendedIds.indexOf(b.getId())))
                    .limit(RECOMMENDED_LIMIT)
                    .map(eventMapper::toResponseDTO)
                    .toList();

            if (recommendedEvents.isEmpty()) {
                return fallbackRecommendations(upcomingEvents);
            }

            return recommendedEvents;
        } catch (Exception e) {
            log.error("AI recommendation failed: {}", e.getMessage());
            return fallbackRecommendations(upcomingEvents);
        }
    }

    private UUID toUuidOrNull(String rawId) {
        try {
            return UUID.fromString(rawId);
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<EventDto> fallbackRecommendations(List<ProgrammingEvent> upcomingEvents) {
        List<ProgrammingEvent> randomized = new ArrayList<>(upcomingEvents);
        Collections.shuffle(randomized);
        return randomized.stream()
                .limit(RECOMMENDED_LIMIT)
                .map(eventMapper::toResponseDTO)
                .toList();
    }
}
