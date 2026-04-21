package com.codearena.user.entity;

public enum Role {
    ADMIN,
    PARTICIPANT,
    COACH;

    public static Role fromAuth0Role(String role) {
        if (role == null) {
            return PARTICIPANT;
        }
        if (role.startsWith("ROLE_")) {
            role = role.substring("ROLE_".length());
        }
        try {
            return Role.valueOf(role);
        } catch (IllegalArgumentException ex) {
            return PARTICIPANT;
        }
    }
}
