package com.codearena.module6_event.mapper;

import com.codearena.module6_event.dto.CreateEventRequest;
import com.codearena.module6_event.dto.EventDto;
import com.codearena.module6_event.dto.RegistrationResponseDTO;
import com.codearena.module6_event.entity.EventRegistration;
import com.codearena.module6_event.entity.ProgrammingEvent;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "currentParticipants", constant = "0")
    @Mapping(target = "createdAt", ignore = true)
    ProgrammingEvent toEntity(CreateEventRequest dto);

    @Mapping(target = "availablePlaces", ignore = true)
    @Mapping(target = "isFull", ignore = true)
    @Mapping(target = "fillRate", ignore = true)
    EventDto toResponseDTO(ProgrammingEvent event);

    @AfterMapping
    default void fillComputedEventFields(ProgrammingEvent event, @MappingTarget EventDto dto) {
        int max = event.getMaxParticipants() == null ? 0 : event.getMaxParticipants();
        int cur = event.getCurrentParticipants() == null ? 0 : event.getCurrentParticipants();
        dto.setAvailablePlaces(Math.max(0, max - cur));
        dto.setIsFull(max > 0 && cur >= max);
        dto.setFillRate(max == 0 ? 0.0 : (cur * 100.0) / max);
    }

    @Mapping(target = "eventId", source = "event.id")
    RegistrationResponseDTO toRegistrationResponseDTO(EventRegistration registration);
}
