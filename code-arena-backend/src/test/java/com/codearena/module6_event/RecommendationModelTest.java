package com.codearena.module6_event;

import com.codearena.module6_event.entity.ProgrammingEvent;
import com.codearena.module6_event.enums.EventCategory;
import com.codearena.module6_event.enums.EventType;
import com.codearena.module6_event.service.recommendation.RecommendationModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RecommendationModelTest {

    private RecommendationModel model = new RecommendationModel();

    @Test
    void cosineSimilarity_shouldReturn1_forIdenticalVectors() {
        double[] v = {1.0, 0.0, 1.0, 0.0};
        double result = model.cosineSimilarity(v, v);
        assertEquals(1.0, result, 0.001);
    }

    @Test
    void cosineSimilarity_shouldReturn0_forOrthogonalVectors() {
        double[] v1 = {1.0, 0.0};
        double[] v2 = {0.0, 1.0};
        double result = model.cosineSimilarity(v1, v2);
        assertEquals(0.0, result, 0.001);
    }

    @Test
    void buildParticipantProfile_shouldReturnDefault_whenNoHistory() {
        double[] profile = model.buildParticipantProfile(List.of());
        assertNotNull(profile);
        assertEquals(8, profile.length);
    }

    @Test
    void scoreEvents_shouldReturnHighScore_forMatchingEvent() {
        ProgrammingEvent hackathon = new ProgrammingEvent();
        hackathon.setId(UUID.randomUUID());
        hackathon.setTitle("Test Hackathon");
        hackathon.setCategory(EventCategory.HACKATHON);
        hackathon.setType(EventType.OPEN);
        hackathon.setMaxParticipants(50);
        hackathon.setCurrentParticipants(10);
        
        double[] hackathonProfile = {1,0,0,0,0,1,0,0.5};
        
        Map<UUID, Double> scores = model.scoreEvents(
            hackathonProfile, List.of(hackathon));
        
        assertTrue(scores.get(hackathon.getId()) > 0.5);
    }
}
