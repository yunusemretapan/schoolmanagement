package com.schoolos.application.features.identity.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.schoolos.domain.identity.LoginIdentity;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Spec 0003, AC-5 boundaries of the per-account lockout. */
class LoginAttemptPolicyTest {

    private static final Instant T0 = Instant.parse("2026-11-19T07:00:00Z");

    private static LoginIdentity fresh() {
        return new LoginIdentity(UUID.randomUUID(), UUID.randomUUID(), "a@b.co", null, "h", 0, null);
    }

    private static LoginIdentity fail(LoginIdentity identity, int times) {
        LoginIdentity current = identity;
        for (int i = 0; i < times; i++) {
            current = LoginAttemptPolicy.afterFailure(current, T0);
        }
        return current;
    }

    @Test
    void fourFailuresDoNotLock() {
        LoginIdentity afterFour = fail(fresh(), 4);

        assertThat(afterFour.failedAttempts()).isEqualTo(4);
        assertThat(LoginAttemptPolicy.isLocked(afterFour, T0)).isFalse();
    }

    @Test
    void successResetsTheCounter() {
        LoginIdentity reset = LoginAttemptPolicy.afterSuccess(fail(fresh(), 4));

        assertThat(reset.failedAttempts()).isZero();
        assertThat(LoginAttemptPolicy.isLocked(fail(reset, 4), T0)).isFalse();
    }

    @Test
    void fifthFailureLocksForFifteenMinutes() {
        LoginIdentity locked = fail(fresh(), 5);

        assertThat(locked.lockedUntil()).isEqualTo(T0.plus(Duration.ofMinutes(15)));
        assertThat(LoginAttemptPolicy.isLocked(locked, T0.plus(Duration.ofSeconds(14 * 60 + 59))))
                .isTrue();
        assertThat(LoginAttemptPolicy.isLocked(locked, T0.plus(Duration.ofMinutes(15))))
                .isFalse();
    }

    @Test
    void afterAnExpiredLockTheCountStartsAgain() {
        LoginIdentity locked = fail(fresh(), 5);
        Instant later = T0.plus(Duration.ofMinutes(16));

        LoginIdentity oneMore = LoginAttemptPolicy.afterFailure(locked, later);

        assertThat(oneMore.failedAttempts()).isEqualTo(1);
        assertThat(LoginAttemptPolicy.isLocked(oneMore, later)).isFalse();
    }
}
