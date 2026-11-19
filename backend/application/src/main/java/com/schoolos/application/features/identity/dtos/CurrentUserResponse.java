package com.schoolos.application.features.identity.dtos;

import java.util.List;
import java.util.UUID;

/** The home screen's data (spec 0003, Req. 10): who is signed in, for which institution, with which roles. */
public record CurrentUserResponse(UUID userId, String fullName, String institutionName, List<String> roles) {

    public CurrentUserResponse {
        roles = List.copyOf(roles);
    }
}
