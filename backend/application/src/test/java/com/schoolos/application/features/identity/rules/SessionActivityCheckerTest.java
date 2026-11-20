package com.schoolos.application.features.identity.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.schoolos.application.features.identity.IdentityFixture;
import com.schoolos.domain.identity.ClientType;
import com.schoolos.domain.identity.Role;
import com.schoolos.domain.identity.Session;
import com.schoolos.domain.identity.UserAccount;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Spec 0004, Req. 7 and 8 / AC-7, AC-8 (plan 0004, R2): access follows the account as it is now. */
class SessionActivityCheckerTest {

    private final IdentityFixture fx = new IdentityFixture();
    private final SessionActivityChecker checker = new SessionActivityChecker(
            fx.sessionRepository(), fx.identityRepository(), fx.userRepository(), fx.transactions, fx.tenantSession());

    private final UserAccount teacher =
            fx.seedUser("Örnek Koleji", "Öğretmen", "ogretmen@ornek.test", null, "Ornek-Sifre-2026", Role.TEACHER);

    private UUID open() {
        UUID id = fx.ids.next();
        fx.sessions.put(
                id,
                new Session(
                        id,
                        teacher.id(),
                        teacher.tenantId(),
                        ClientType.WEB,
                        "h",
                        fx.clock.instant(),
                        fx.clock.instant(),
                        null));
        return id;
    }

    @Test
    void anOpenSessionGetsTheAccountsCurrentRoles() {
        UUID session = open();

        assertThat(checker.currentRoles(session, teacher.tenantId())).contains(Set.of("TEACHER"));
        assertThat(fx.boundTenant()).contains(teacher.tenantId());
    }

    @Test
    void aRoleAddedOrRemovedCountsFromTheNextRequest() {
        UUID session = open();
        fx.users.put(teacher.id(), teacher.withRoles(Set.of(Role.TEACHER, Role.ADMIN)));

        assertThat(checker.currentRoles(session, teacher.tenantId())).contains(Set.of("TEACHER", "ADMIN"));

        fx.users.put(teacher.id(), teacher.withRoles(Set.of(Role.ACCOUNTANT)));

        assertThat(checker.currentRoles(session, teacher.tenantId())).contains(Set.of("ACCOUNTANT"));
    }

    @Test
    void aDeactivatedAccountsOpenSessionGetsNothing() {
        UUID session = open();
        fx.identities.put(teacher.id(), fx.identities.get(teacher.id()).deactivated());

        assertThat(checker.currentRoles(session, teacher.tenantId())).isEmpty();
    }

    @Test
    void aClosedOrUnknownSessionGetsNothing() {
        UUID session = open();
        fx.sessions.put(session, fx.sessions.get(session).revoked(fx.clock.instant()));

        assertThat(checker.currentRoles(session, teacher.tenantId())).isEmpty();
        assertThat(checker.currentRoles(UUID.randomUUID(), teacher.tenantId())).isEmpty();
    }

    @Test
    void aSessionOfAnotherTenantGetsNothing() {
        UUID session = open();

        assertThat(checker.currentRoles(session, UUID.randomUUID())).isEmpty();
    }
}
