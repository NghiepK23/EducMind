package com.educmind.assessment.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.UUID;

final class Actor {

    private Actor() {
    }

    static UUID id(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    static String role(Authentication authentication) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String value = authority.getAuthority();
            if (value != null && value.startsWith("ROLE_")) {
                return value.substring("ROLE_".length());
            }
        }
        return "";
    }
}
