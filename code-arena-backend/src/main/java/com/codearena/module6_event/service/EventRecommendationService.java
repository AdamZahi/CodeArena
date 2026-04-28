package com.codearena.module6_event.service;

import com.codearena.module6_event.dto.EventDto;
import com.codearena.module6_event.entity.EventRegistration;
import com.codearena.module6_event.entity.ProgrammingEvent;
import com.codearena.module6_event.mapper.EventMapper;
import com.codearena.module6_event.repository.EventRegistrationRepository;
import com.codearena.module6_event.repository.EventRepository;
import com.codearena.module6_event.service.recommendation.EventFeatureVector;
import com.codearena.module6_event.service.recommendation.RecommendationModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventRecommendationService {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final RecommendationModel recommendationModel;
    private final EventMapper eventMapper;

    public List<EventDto> getRecommendedEvents(String participantId) {
        
        // 1. Get participant's confirmed event history
        List<EventRegistration> history = 
            registrationRepository.findByParticipantId(participantId)
            .stream()
            .filter(r -> r.getStatus().name().equals("CONFIRMED"))
            .toList();
        
        List<ProgrammingEvent> participantHistory = history.stream()
            .map(EventRegistration::getEvent)
            .toList();
        
        log.info("Participant {} has {} events in history",
            participantId, participantHistory.size());
        
        // 2. Get upcoming events not yet joined by participant
        Set<UUID> joinedEventIds = history.stream()
            .map(r -> r.getEvent().getId())
            .collect(Collectors.toSet());
        
        List<ProgrammingEvent> candidateEvents = eventRepository
            .findAll()
            .stream()
            .filter(e -> e.getStartDate().isAfter(LocalDateTime.now()))
            .filter(e -> !joinedEventIds.contains(e.getId()))
            .filter(e -> !e.isFull())
            .toList();
        
        if (candidateEvents.isEmpty()) {
            log.info("No candidate events available");
            return List.of();
        }
        
        // 3. Build participant profile using the model
        double[] participantProfile = recommendationModel
            .buildParticipantProfile(participantHistory);
        
        // 4. Score all candidate events
        Map<UUID, Double> scores = recommendationModel
            .scoreEvents(participantProfile, candidateEvents);
        
        // 5. Return top 3 events sorted by score
        return candidateEvents.stream()
            .sorted((a, b) -> Double.compare(
                scores.getOrDefault(b.getId(), 0.0),
                scores.getOrDefault(a.getId(), 0.0)
            ))
            .limit(3)
            .map(eventMapper::toResponseDTO)
            .toList();
    }
    
    /**
     * Get recommendation explanation for a specific event.
     * Shows why this event was recommended.
     */
    public Map<String, Object> getRecommendationExplanation(
            String participantId, UUID eventId) {
        
        List<EventRegistration> history = 
            registrationRepository.findByParticipantId(participantId);
        
        List<ProgrammingEvent> participantHistory = history.stream()
            .map(EventRegistration::getEvent)
            .toList();
        
        ProgrammingEvent event = eventRepository.findById(eventId)
            .orElseThrow();
        
        double[] profile = recommendationModel
            .buildParticipantProfile(participantHistory);
        double[] eventVector = EventFeatureVector.toVector(event);
        double score = recommendationModel
            .cosineSimilarity(profile, eventVector);
        
        Map<String, Object> explanation = new LinkedHashMap<>();
        explanation.put("eventTitle", event.getTitle());
        explanation.put("similarityScore", 
            Math.round(score * 100) + "%");
        explanation.put("basedOnHistory", 
            participantHistory.stream()
                .map(ProgrammingEvent::getTitle)
                .toList());
        explanation.put("matchingCategory", 
            event.getCategory().name());
        explanation.put("recommendation", 
            score > 0.7 ? "HIGHLY RECOMMENDED" :
            score > 0.4 ? "RECOMMENDED" : "SUGGESTED");
        
        return explanation;
    }
}
