package com.schoolos.application.features.identity.commands;

import com.schoolos.application.features.identity.ports.SessionRepository;
import com.schoolos.application.features.identity.rules.RefreshTokens;
import com.schoolos.core.application.mediator.RequestHandler;
import com.schoolos.core.security.CurrentUser;
import com.schoolos.core.security.CurrentUserHolder;
import com.schoolos.domain.identity.Session;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

/**
 * Revokes this device's session at once; the user's other devices keep theirs (spec 0003, AC-9). With a valid access
 * token its session is ended; without one, the session whose current refresh secret was presented. A refresh token
 * that does not match proves nothing, so it ends nothing. Always succeeds: signing out never fails for the user.
 */
public final class LogoutCommandHandler implements RequestHandler<LogoutCommand, Void> {

    private final SessionRepository sessions;
    private final Clock clock;

    public LogoutCommandHandler(SessionRepository sessions, Clock clock) {
        this.sessions = sessions;
        this.clock = clock;
    }

    @Override
    public Class<LogoutCommand> requestType() {
        return LogoutCommand.class;
    }

    @Override
    public Void handle(LogoutCommand command) {
        Optional<UUID> fromAccessToken = CurrentUserHolder.current().map(CurrentUser::sessionId);
        Optional<Session> session = fromAccessToken.isPresent()
                ? fromAccessToken.flatMap(sessions::findById)
                : RefreshTokens.parse(command.refreshToken())
                        .flatMap(parsed -> sessions.findById(parsed.sessionId())
                                .filter(found -> RefreshTokens.matches(parsed.secret(), found.tokenHash())));
        session.filter(found -> !found.isRevoked()).ifPresent(found -> sessions.update(found.revoked(clock.instant())));
        return null;
    }
}
