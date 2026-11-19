package com.schoolos.application.features.identity.commands;

import com.schoolos.application.features.identity.constants.IdentityErrorCodes;
import com.schoolos.application.features.identity.dtos.AuthOutcome;
import com.schoolos.application.features.identity.ports.SecretGenerator;
import com.schoolos.application.features.identity.ports.SessionRepository;
import com.schoolos.application.features.identity.ports.UserRepository;
import com.schoolos.application.features.identity.rules.RefreshTokens;
import com.schoolos.application.features.identity.rules.SessionPolicy;
import com.schoolos.core.application.behaviors.TenantSessionPort;
import com.schoolos.core.application.mediator.RequestHandler;
import com.schoolos.core.security.TokenService;
import com.schoolos.domain.identity.Session;
import com.schoolos.domain.identity.UserAccount;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Spec 0003, Req. 6 and AC-8/AC-9. A valid refresh slides the session's idle limit and rotates the secret. A secret
 * that is not the session's current one means an old token came back: the whole session is revoked, the latest
 * rotation included.
 */
public final class RefreshSessionCommandHandler implements RequestHandler<RefreshSessionCommand, AuthOutcome> {

    private static final AuthOutcome.Refused EXPIRED = new AuthOutcome.Refused(IdentityErrorCodes.SESSION_EXPIRED);

    private final SessionRepository sessions;
    private final UserRepository users;
    private final TenantSessionPort tenantSession;
    private final SecretGenerator secrets;
    private final Clock clock;
    private final SessionTokenIssuer issuer;

    public RefreshSessionCommandHandler(
            SessionRepository sessions,
            UserRepository users,
            TokenService tokens,
            TenantSessionPort tenantSession,
            SecretGenerator secrets,
            Clock clock) {
        this.sessions = sessions;
        this.users = users;
        this.tenantSession = tenantSession;
        this.secrets = secrets;
        this.clock = clock;
        this.issuer = new SessionTokenIssuer(tokens);
    }

    @Override
    public Class<RefreshSessionCommand> requestType() {
        return RefreshSessionCommand.class;
    }

    @Override
    public AuthOutcome handle(RefreshSessionCommand command) {
        Optional<RefreshTokens.Parsed> parsed = RefreshTokens.parse(command.refreshToken());
        if (parsed.isEmpty()) {
            return EXPIRED;
        }
        Optional<Session> found = sessions.lockById(parsed.get().sessionId());
        if (found.isEmpty() || found.get().isRevoked()) {
            return EXPIRED;
        }
        Session session = found.get();
        Instant now = clock.instant();
        if (!RefreshTokens.matches(parsed.get().secret(), session.tokenHash())) {
            sessions.update(session.revoked(now));
            return EXPIRED;
        }
        if (SessionPolicy.isExpired(session, now)) {
            return EXPIRED;
        }
        String secret = secrets.newSecret();
        Session rotated = session.rotated(RefreshTokens.hash(secret), now);
        sessions.update(rotated);

        tenantSession.bind(session.tenantId());
        Optional<UserAccount> user = users.findById(session.userId());
        if (user.isEmpty()) {
            return EXPIRED;
        }
        return issuer.issue(user.get(), rotated, secret);
    }
}
