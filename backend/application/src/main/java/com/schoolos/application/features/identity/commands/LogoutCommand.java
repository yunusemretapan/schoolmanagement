package com.schoolos.application.features.identity.commands;

import com.schoolos.core.application.mediator.Request;
import com.schoolos.core.security.Public;

/**
 * End this device's session only (spec 0003, Req. 7). The session is found from the access token when it is still
 * valid, otherwise from the refresh token (cookie or body), so signing out works after the 15-minute access token
 * expired (review 1, H-1). Public: it can only end a session whose access token or current refresh secret the caller
 * holds, and it ends nothing else.
 */
@Public
public record LogoutCommand(String refreshToken) implements Request<Void> {

    @Override
    public String toString() {
        return "LogoutCommand[]"; // never log the token
    }
}
