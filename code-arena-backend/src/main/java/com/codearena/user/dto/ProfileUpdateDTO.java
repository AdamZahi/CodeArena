/*
 * MERGED: ProfileUpdateDTO.java
 * Using TARGET version — supports firstName, lastName, nickname, email, bio, avatarUrl
 */
package com.codearena.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateDTO {
    private String firstName;
    private String lastName;
    private String nickname;
    private String email;
    private String bio;
    private String avatarUrl;
}
