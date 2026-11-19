package com.schoolos.application.features.identity.commands;

import com.schoolos.application.features.identity.dtos.AuthOutcome;
import com.schoolos.core.application.mediator.Request;
import com.schoolos.core.security.Public;
import com.schoolos.domain.identity.ClientType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Sign in with an e-mail or phone number and a password (spec 0003, Req. 2–5). {@code source} is the caller's network
 * address, set by the web layer for the per-source rate limit; it is never taken from the request body.
 */
@Public
public record LoginCommand(
        @NotBlank String identifier,
        @NotBlank @Size(max = 128) String password,
        @NotNull ClientType clientType,
        @NotBlank String source)
        implements Request<AuthOutcome> {

    @Override
    public String toString() {
        return "LoginCommand[clientType=" + clientType + "]"; // no identifier or password in logs
    }
}
