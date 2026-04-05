package com.codearena.user.mapper;

import com.codearena.user.dto.ProfileUpdateDTO;
import com.codearena.user.dto.UserRequestDTO;
import com.codearena.user.dto.UserResponseDTO;
import com.codearena.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponseDTO toResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    User toEntity(UserRequestDTO request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "keycloakId", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "firstName", source = "firstName")
    @Mapping(target = "lastName", source = "lastName")
    @Mapping(target = "nickname", source = "nickname")
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "authProvider", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateProfile(ProfileUpdateDTO request, @MappingTarget User user);
}
