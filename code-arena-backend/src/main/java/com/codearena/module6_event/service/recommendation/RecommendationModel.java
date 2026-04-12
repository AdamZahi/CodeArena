package com.codearena.module6_event.service.recommendation;

import com.codearena.module6_event.entity.ProgrammingEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class RecommendationModel {

    /**
     * Cosine similarity between two vectors.
     * Returns value between 0 (no similarity) and 1 (identical)
     */
    public double cosineSimilarity(double[] vectorA, double[] vectorB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        
        if (normA == 0 || normB == 0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Build participant profile vector from their event history.
     * Average of all event vectors they participated in.
     */
    public double[] buildParticipantProfile(
            List<ProgrammingEvent> participantHistory) {
        
        if (participantHistory.isEmpty()) {
            // No history - return neutral profile
            return new double[]{0.2, 0.2, 0.2, 0.2, 0.2, 0.5, 0.5, 0.5};
        }
        
        double[] profile = new double[8];
        
        for (ProgrammingEvent event : participantHistory) {
            double[] vector = EventFeatureVector.toVector(event);
            for (int i = 0; i < vector.length; i++) {
                profile[i] += vector[i];
            }
        }
        
        // Average
        for (int i = 0; i < profile.length; i++) {
            profile[i] /= participantHistory.size();
        }
        
        log.info("Built participant profile vector: {}", 
            Arrays.toString(profile));
        return profile;
    }

    /**
     * Score all events against participant profile.
     * Returns map of eventId -> similarity score.
     */
    public Map<UUID, Double> scoreEvents(
            double[] participantProfile,
            List<ProgrammingEvent> candidateEvents) {
        
        Map<UUID, Double> scores = new HashMap<>();
        
        for (ProgrammingEvent event : candidateEvents) {
            double[] eventVector = EventFeatureVector.toVector(event);
            double score = cosineSimilarity(participantProfile, eventVector);
            scores.put(event.getId(), score);
            log.info("Event '{}' similarity score: {}", 
                event.getTitle(), score);
        }
        
        return scores;
    }
}
