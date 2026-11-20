package com.schoolos.application.features.identity.dtos;

import java.time.Instant;
import java.util.UUID;

/**
 * A fresh invitation link's secret part, shown once to the administrator (spec 0004, Req. 5). The web app builds
 * the address from it ({@code /davet#<token>}: plan 0004, R4); the server never stores the token itself.
 */
public record InvitationLink(UUID staffId, String token, Instant expiresAt) {

    @Override
    public String toString() {
        return "InvitationLink[staffId=" + staffId + "]"; // no token in logs
    }
}
