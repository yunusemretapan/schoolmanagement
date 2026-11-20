package com.schoolos.webapi.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** Spec 0003, AC-5: at most 20 login attempts per source per minute, whichever accounts they target. */
class InMemorySourceRateLimiterTest {

    private static final class SteppingClock extends Clock {
        private Instant now = Instant.parse("2026-11-19T07:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final SteppingClock clock = new SteppingClock();
    private final InMemorySourceRateLimiter limiter = new InMemorySourceRateLimiter(clock);

    @Test
    void twentiethAttemptIsServedTheTwentyFirstIsRefused() {
        for (int i = 1; i <= 20; i++) {
            assertThat(limiter.tryAcquire("203.0.113.7")).as("attempt %d", i).isTrue();
            clock.advance(Duration.ofSeconds(1));
        }
        assertThat(limiter.tryAcquire("203.0.113.7")).isFalse();
    }

    @Test
    void otherSourcesHaveTheirOwnBudget() {
        for (int i = 0; i < 20; i++) {
            limiter.tryAcquire("203.0.113.7");
        }
        assertThat(limiter.tryAcquire("198.51.100.1")).isTrue();
    }

    @Test
    void theWindowSlides() {
        for (int i = 0; i < 20; i++) {
            limiter.tryAcquire("203.0.113.7");
        }
        clock.advance(Duration.ofSeconds(59));
        assertThat(limiter.tryAcquire("203.0.113.7")).isFalse();

        clock.advance(Duration.ofSeconds(1));
        assertThat(limiter.tryAcquire("203.0.113.7")).isTrue();
    }

    /** Spec 0004, G4 / AC-5: invitation tries use their own bucket ("invitation:" + source, InvitationGate). */
    @Test
    void invitationTriesAndSignInsOfOneSourceAreCountedApart() {
        for (int i = 0; i < 20; i++) {
            assertThat(limiter.tryAcquire("invitation:203.0.113.7")).isTrue();
        }
        assertThat(limiter.tryAcquire("invitation:203.0.113.7")).isFalse();
        assertThat(limiter.tryAcquire("203.0.113.7")).isTrue();

        clock.advance(Duration.ofMinutes(1));
        assertThat(limiter.tryAcquire("invitation:203.0.113.7")).isTrue();
    }
}
