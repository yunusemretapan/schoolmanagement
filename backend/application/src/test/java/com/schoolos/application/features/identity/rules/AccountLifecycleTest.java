package com.schoolos.application.features.identity.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.schoolos.domain.identity.AccountStatus;
import com.schoolos.domain.identity.AuditAction;
import com.schoolos.domain.identity.AuditEvent;
import com.schoolos.domain.identity.Invitation;
import com.schoolos.domain.identity.LoginIdentity;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The domain's account and invitation states (spec 0004, Req. 5, 6, 8; G5) and the audit record's shape (AC-10). */
class AccountLifecycleTest {

    private static final UUID USER = UUID.randomUUID();
    private static final UUID TENANT = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-20T09:00:00Z");

    @Test
    void anInvitedAccountHasNoPasswordAndCannotSignIn() {
        LoginIdentity invited = LoginIdentity.invited(USER, TENANT, "a@ornek.test", null);

        assertThat(invited.status()).isEqualTo(AccountStatus.INVITED);
        assertThat(invited.passwordHash()).isNull();
        assertThat(invited.canSignIn()).isFalse();
    }

    @Test
    void anActiveAccountWithoutAPasswordCannotExist() {
        assertThatThrownBy(() -> new LoginIdentity(USER, TENANT, "a@ornek.test", null, null, 0, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptingSetsThePasswordAndActivates() {
        LoginIdentity active =
                LoginIdentity.invited(USER, TENANT, "a@ornek.test", null).activatedWith("h");

        assertThat(active.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(active.canSignIn()).isTrue();
    }

    @Test
    void reactivationReturnsToActiveOnlyWithAPasswordOtherwiseToInvited() {
        LoginIdentity wasActive = LoginIdentity.invited(USER, TENANT, "a@ornek.test", null)
                .activatedWith("h")
                .deactivated();
        LoginIdentity neverAccepted =
                LoginIdentity.invited(USER, TENANT, "a@ornek.test", null).deactivated();

        assertThat(wasActive.canSignIn()).isFalse();
        assertThat(wasActive.reactivated().status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(neverAccepted.reactivated().status()).isEqualTo(AccountStatus.INVITED);
    }

    @Test
    void anInvitationIsUsableUntilItExpiresIsUsedOrIsRevoked() {
        Invitation fresh =
                new Invitation(UUID.randomUUID(), USER, TENANT, "h", NOW, NOW.plus(Duration.ofHours(72)), null, null);

        assertThat(fresh.isUsable(NOW.plus(Duration.ofHours(72).minusMinutes(1))))
                .isTrue();
        assertThat(fresh.isUsable(NOW.plus(Duration.ofHours(72)))).isFalse();
        assertThat(fresh.used(NOW).isUsable(NOW)).isFalse();
        assertThat(fresh.revoked(NOW).isUsable(NOW)).isFalse();
    }

    @Test
    void anAuditEventAlwaysNamesWhoWhomWhatAndWhen() {
        assertThatThrownBy(
                        () -> new AuditEvent(UUID.randomUUID(), TENANT, null, USER, AuditAction.ROLE_ADDED, null, NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuditEvent(UUID.randomUUID(), TENANT, USER, USER, null, null, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
