package com.codearena.module6_event;

import com.codearena.module6_event.dto.CandidatureRequestDTO;
import com.codearena.module6_event.dto.CreateEventRequest;
import com.codearena.module6_event.enums.EventCategory;
import com.codearena.module6_event.enums.EventType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory =
            Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // CreateEventRequest tests
    @Test
    void createEvent_shouldFail_whenTitleBlank() {
        CreateEventRequest req = new CreateEventRequest();
        req.setTitle("");
        req.setDescription("desc");
        req.setStartDate(LocalDateTime.now().plusDays(1));
        req.setEndDate(LocalDateTime.now().plusDays(2));
        req.setMaxParticipants(50);
        req.setType(EventType.OPEN);
        req.setCategory(EventCategory.HACKATHON);

        Set<ConstraintViolation<CreateEventRequest>> violations =
            validator.validate(req);

        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath()
                .toString().equals("title")));
    }

    @Test
    void createEvent_shouldFail_whenTitleTooShort() {
        CreateEventRequest req = new CreateEventRequest();
        req.setTitle("ab"); // less than 3 chars
        req.setDescription("desc");
        req.setStartDate(LocalDateTime.now().plusDays(1));
        req.setEndDate(LocalDateTime.now().plusDays(2));
        req.setMaxParticipants(50);
        req.setType(EventType.OPEN);
        req.setCategory(EventCategory.HACKATHON);

        Set<ConstraintViolation<CreateEventRequest>> violations =
            validator.validate(req);

        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath()
                .toString().equals("title")));
    }

    @Test
    void createEvent_shouldFail_whenStartDateInPast() {
        CreateEventRequest req = new CreateEventRequest();
        req.setTitle("Valid Title");
        req.setDescription("desc");
        req.setStartDate(LocalDateTime.now().minusDays(1)); // past
        req.setEndDate(LocalDateTime.now().plusDays(2));
        req.setMaxParticipants(50);
        req.setType(EventType.OPEN);
        req.setCategory(EventCategory.HACKATHON);

        Set<ConstraintViolation<CreateEventRequest>> violations =
            validator.validate(req);

        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath()
                .toString().equals("startDate")));
    }

    @Test
    void createEvent_shouldFail_whenMaxParticipantsZero() {
        CreateEventRequest req = new CreateEventRequest();
        req.setTitle("Valid Title");
        req.setDescription("desc");
        req.setStartDate(LocalDateTime.now().plusDays(1));
        req.setEndDate(LocalDateTime.now().plusDays(2));
        req.setMaxParticipants(0); // invalid
        req.setType(EventType.OPEN);
        req.setCategory(EventCategory.HACKATHON);

        Set<ConstraintViolation<CreateEventRequest>> violations =
            validator.validate(req);

        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath()
                .toString().equals("maxParticipants")));
    }

    @Test
    void createEvent_shouldFail_whenMaxParticipantsOver1000() {
        CreateEventRequest req = new CreateEventRequest();
        req.setTitle("Valid Title");
        req.setDescription("desc");
        req.setStartDate(LocalDateTime.now().plusDays(1));
        req.setEndDate(LocalDateTime.now().plusDays(2));
        req.setMaxParticipants(1001); // over max
        req.setType(EventType.OPEN);
        req.setCategory(EventCategory.HACKATHON);

        Set<ConstraintViolation<CreateEventRequest>> violations =
            validator.validate(req);

        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath()
                .toString().equals("maxParticipants")));
    }

    @Test
    void createEvent_shouldPass_whenAllValid() {
        CreateEventRequest req = new CreateEventRequest();
        req.setTitle("Valid Hackathon");
        req.setDescription("Valid description");
        req.setStartDate(LocalDateTime.now().plusDays(1));
        req.setEndDate(LocalDateTime.now().plusDays(2));
        req.setMaxParticipants(50);
        req.setType(EventType.OPEN);
        req.setCategory(EventCategory.HACKATHON);

        Set<ConstraintViolation<CreateEventRequest>> violations =
            validator.validate(req);

        assertTrue(violations.isEmpty());
    }

    // CandidatureRequestDTO tests
    @Test
    void candidature_shouldFail_whenMotivationBlank() {
        CandidatureRequestDTO dto = new CandidatureRequestDTO();
        dto.setMotivation("");

        Set<ConstraintViolation<CandidatureRequestDTO>> violations =
            validator.validate(dto);

        assertFalse(violations.isEmpty());
    }

    @Test
    void candidature_shouldFail_whenMotivationTooShort() {
        CandidatureRequestDTO dto = new CandidatureRequestDTO();
        dto.setMotivation("Too short"); // less than 20 chars

        Set<ConstraintViolation<CandidatureRequestDTO>> violations =
            validator.validate(dto);

        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath()
                .toString().equals("motivation")));
    }

    @Test
    void candidature_shouldFail_whenMotivationTooLong() {
        CandidatureRequestDTO dto = new CandidatureRequestDTO();
        dto.setMotivation("a".repeat(501)); // over 500 chars

        Set<ConstraintViolation<CandidatureRequestDTO>> violations =
            validator.validate(dto);

        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath()
                .toString().equals("motivation")));
    }

    @Test
    void candidature_shouldPass_whenMotivationValid() {
        CandidatureRequestDTO dto = new CandidatureRequestDTO();
        dto.setMotivation(
            "I am passionate about coding and want to join " +
            "this hackathon to improve my skills."
        );

        Set<ConstraintViolation<CandidatureRequestDTO>> violations =
            validator.validate(dto);

        assertTrue(violations.isEmpty());
    }
}
