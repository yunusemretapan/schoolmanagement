package com.schoolos.application.features.identity.commands;

import com.schoolos.application.features.identity.constants.IdentityErrorCodes;
import com.schoolos.application.features.identity.dtos.AuthOutcome;
import com.schoolos.application.features.identity.ports.LoginIdentityRepository;
import com.schoolos.application.features.identity.ports.SecretGenerator;
import com.schoolos.application.features.identity.ports.SessionRepository;
import com.schoolos.application.features.identity.ports.SourceRateLimiter;
import com.schoolos.application.features.identity.ports.UserRepository;
import com.schoolos.application.features.identity.rules.LoginAttemptPolicy;
import com.schoolos.application.features.identity.rules.LoginIdentifier;
import com.schoolos.application.features.identity.rules.RefreshTokens;
import com.schoolos.core.application.behaviors.TenantSessionPort;
import com.schoolos.core.application.mediator.RequestHandler;
import com.schoolos.core.crosscutting.exceptions.TooManyRequestsException;
import com.schoolos.core.crosscutting.ids.IdGenerator;
import com.schoolos.core.security.PasswordHasher;
import com.schoolos.core.security.TokenService;
import com.schoolos.domain.identity.LoginIdentity;
import com.schoolos.domain.identity.Session;
import com.schoolos.domain.identity.UserAccount;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Spec 0003, Req. 2–6. Unknown identifier, wrong password and locked account all end in the same refusal
 * (Req. 4); a password is hashed in every case so the answer takes as long either way. Only after the password
 * matched does the transaction bind to the user's tenant to read their roles.
 */
public final class LoginCommandHandler implements RequestHandler<LoginCommand, AuthOutcome> {

    private static final AuthOutcome.Refused REFUSED = new AuthOutcome.Refused(IdentityErrorCodes.INVALID_CREDENTIALS);

    private final LoginIdentityRepository identities;
    private final UserRepository users;
    private final SessionRepository sessions;
    private final SourceRateLimiter rateLimiter;
    private final PasswordHasher passwords;
    private final TenantSessionPort tenantSession;
    private final SecretGenerator secrets;
    private final IdGenerator ids;
    private final Clock clock;
    private final SessionTokenIssuer issuer;
    private final String timingDecoy;

    public LoginCommandHandler(
            LoginIdentityRepository identities,
            UserRepository users,
            SessionRepository sessions,
            SourceRateLimiter rateLimiter,
            PasswordHasher passwords,
            TokenService tokens,
            TenantSessionPort tenantSession,
            SecretGenerator secrets,
            IdGenerator ids,
            Clock clock) {
        this.identities = identities;
        this.users = users;
        this.sessions = sessions;
        this.rateLimiter = rateLimiter;
        this.passwords = passwords;
        this.tenantSession = tenantSession;
        this.secrets = secrets;
        this.ids = ids;
        this.clock = clock;
        this.issuer = new SessionTokenIssuer(tokens);
        this.timingDecoy = passwords.hash("timing-decoy-never-a-real-password");
    }

    @Override
    public Class<LoginCommand> requestType() {
        return LoginCommand.class;
    }

    @Override
    public AuthOutcome handle(LoginCommand command) {
        if (!rateLimiter.tryAcquire(command.source())) {
            throw new TooManyRequestsException(IdentityErrorCodes.RATE_LIMITED);
        }
        Instant now = clock.instant();
        Optional<LoginIdentity> found =
                LoginIdentifier.parse(command.identifier()).flatMap(this::find);
        if (found.isEmpty()) {
            passwords.matches(command.password(), timingDecoy);
            return REFUSED;
        }
        LoginIdentity identity = found.get();
        if (LoginAttemptPolicy.isLocked(identity, now)) {
            passwords.matches(command.password(), timingDecoy);
            return REFUSED;
        }
        if (!passwords.matches(command.password(), identity.passwordHash())) {
            identities.update(LoginAttemptPolicy.afterFailure(identity, now));
            return REFUSED;
        }
        identities.update(LoginAttemptPolicy.afterSuccess(identity));

        tenantSession.bind(identity.tenantId());
        Optional<UserAccount> user = users.findById(identity.userId());
        if (user.isEmpty()) {
            return REFUSED;
        }
        String secret = secrets.newSecret();
        Session session = new Session(
                ids.next(),
                identity.userId(),
                identity.tenantId(),
                command.clientType(),
                RefreshTokens.hash(secret),
                now,
                now,
                null);
        sessions.create(session);
        return issuer.issue(user.get(), session, secret);
    }

    private Optional<LoginIdentity> find(LoginIdentifier identifier) {
        return switch (identifier.kind()) {
            case EMAIL -> identities.lockByEmail(identifier.value());
            case PHONE -> identities.lockByPhone(identifier.value());
        };
    }
}
