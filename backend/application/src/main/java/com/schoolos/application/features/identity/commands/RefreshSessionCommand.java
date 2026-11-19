package com.schoolos.application.features.identity.commands;

import com.schoolos.application.features.identity.dtos.AuthOutcome;
import com.schoolos.core.application.mediator.Request;
import com.schoolos.core.security.Public;

/**
 * Trade a refresh token for a new access token and the next refresh token (rotation). Public: the caller's access
 * token may already have expired. A blank token is refused by the handler, not by validation, so every bad token
 * gets the same answer.
 */
@Public
public record RefreshSessionCommand(String refreshToken) implements Request<AuthOutcome> {

    @Override
    public String toString() {
        return "RefreshSessionCommand[]"; // never log the token
    }
}
