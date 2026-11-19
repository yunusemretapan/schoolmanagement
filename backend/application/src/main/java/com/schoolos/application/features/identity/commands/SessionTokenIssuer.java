package com.schoolos.application.features.identity.commands;

import com.schoolos.application.features.identity.dtos.AuthOutcome;
import com.schoolos.application.features.identity.rules.RefreshTokens;
import com.schoolos.core.security.TokenService;
import com.schoolos.domain.identity.Role;
import com.schoolos.domain.identity.Session;
import com.schoolos.domain.identity.UserAccount;
import java.util.Set;
import java.util.stream.Collectors;

/** Builds the token pair for a session: login and refresh share it. */
final class SessionTokenIssuer {

    private final TokenService tokens;

    SessionTokenIssuer(TokenService tokens) {
        this.tokens = tokens;
    }

    AuthOutcome.Issued issue(UserAccount user, Session session, String refreshSecret) {
        Set<String> roles = user.roles().stream().map(Role::name).collect(Collectors.toUnmodifiableSet());
        String access = tokens.issue(user.id(), user.tenantId(), session.id(), roles);
        return new AuthOutcome.Issued(
                access,
                TokenService.ACCESS_TOKEN_TTL.toSeconds(),
                RefreshTokens.format(session.id(), refreshSecret),
                session.clientType());
    }
}
