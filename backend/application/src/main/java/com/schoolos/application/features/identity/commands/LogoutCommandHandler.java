package com.schoolos.application.features.identity.commands;

import com.schoolos.application.features.identity.ports.SessionRepository;
import com.schoolos.core.application.mediator.RequestHandler;
import com.schoolos.core.security.CurrentUser;
import com.schoolos.core.security.CurrentUserHolder;
import com.schoolos.core.security.UnauthenticatedException;
import java.time.Clock;

/** Revokes the current session at once; the user's other devices keep theirs (spec 0003, AC-9). */
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
        CurrentUser user = CurrentUserHolder.current().orElseThrow(() -> new UnauthenticatedException("No session"));
        sessions.findById(user.sessionId())
                .filter(session -> !session.isRevoked())
                .ifPresent(session -> sessions.update(session.revoked(clock.instant())));
        return null;
    }
}
