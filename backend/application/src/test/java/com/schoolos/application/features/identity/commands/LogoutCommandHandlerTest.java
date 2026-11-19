package com.schoolos.application.features.identity.commands;

import static org.assertj.core.api.Assertions.assertThat;

import com.schoolos.application.features.identity.IdentityFixture;
import com.schoolos.application.features.identity.rules.RefreshTokens;
import com.schoolos.application.features.identity.rules.SessionActivityChecker;
import com.schoolos.core.security.CurrentUser;
import com.schoolos.core.security.CurrentUserHolder;
import com.schoolos.core.security.Permission;
import com.schoolos.domain.identity.ClientType;
import com.schoolos.domain.identity.Role;
import com.schoolos.domain.identity.Session;
import com.schoolos.domain.identity.UserAccount;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Spec 0003, AC-9: logout ends this device's session at once and leaves the user's other devices alone. */
class LogoutCommandHandlerTest {

    private final IdentityFixture fx = new IdentityFixture();
    private final LogoutCommandHandler handler = new LogoutCommandHandler(fx.sessionRepository(), fx.clock);
    private final SessionActivityChecker checker = new SessionActivityChecker(fx.sessionRepository());

    private final UserAccount admin =
            fx.seedUser("Örnek Koleji", "Kurucu", "kurucu@ornek.test", null, "Ornek-Sifre-2026", Role.ADMIN);

    @AfterEach
    void clearUser() {
        CurrentUserHolder.clear();
    }

    private UUID open(ClientType type) {
        UUID id = fx.ids.next();
        fx.sessions.put(
                id,
                new Session(id, admin.id(), admin.tenantId(), type, "h", fx.clock.instant(), fx.clock.instant(), null));
        return id;
    }

    @Test
    void endsOnlyTheCallersSession() {
        UUID laptop = open(ClientType.WEB);
        UUID phone = open(ClientType.MOBILE);
        CurrentUserHolder.set(
                new CurrentUser(admin.id(), admin.tenantId(), laptop, Set.of("ADMIN"), Set.of(Permission.SESSION_END)));

        handler.handle(new LogoutCommand(null));

        assertThat(fx.sessions.get(laptop).revokedAt()).isEqualTo(fx.clock.instant());
        assertThat(checker.isActive(laptop)).isFalse();
        assertThat(checker.isActive(phone)).isTrue();
    }

    @Test
    void withAnExpiredAccessTokenTheRefreshTokenEndsTheSessionReview1H1() {
        UUID laptop = open(ClientType.WEB);
        fx.sessions.put(laptop, fx.sessions.get(laptop).rotated(RefreshTokens.hash("current"), fx.clock.instant()));

        handler.handle(new LogoutCommand(RefreshTokens.format(laptop, "current")));

        assertThat(checker.isActive(laptop)).isFalse();
    }

    @Test
    void aRefreshTokenThatDoesNotMatchEndsNothing() {
        UUID laptop = open(ClientType.WEB);
        fx.sessions.put(laptop, fx.sessions.get(laptop).rotated(RefreshTokens.hash("current"), fx.clock.instant()));

        handler.handle(new LogoutCommand(RefreshTokens.format(laptop, "guessed")));
        handler.handle(new LogoutCommand("garbage"));
        handler.handle(new LogoutCommand(null));

        assertThat(checker.isActive(laptop)).isTrue();
    }

    @Test
    void unknownSessionsAreNotActive() {
        assertThat(checker.isActive(UUID.randomUUID())).isFalse();
    }
}
