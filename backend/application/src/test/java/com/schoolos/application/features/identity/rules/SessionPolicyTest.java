package com.schoolos.application.features.identity.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.schoolos.domain.identity.ClientType;
import com.schoolos.domain.identity.Session;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Spec 0003, AC-8: idle limits, measured from the last use. */
class SessionPolicyTest {

    private static final Instant LAST_USE = Instant.parse("2026-11-19T07:00:00Z");

    private static Session session(ClientType type, Instant lastUsedAt) {
        return new Session(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                type,
                "h",
                LAST_USE.minusSeconds(60),
                lastUsedAt,
                null);
    }

    @Test
    void webSessionLivesTwelveHoursAfterLastUse() {
        Session web = session(ClientType.WEB, LAST_USE);

        assertThat(SessionPolicy.isExpired(
                        web, LAST_USE.plus(Duration.ofHours(11).plusMinutes(59))))
                .isFalse();
        assertThat(SessionPolicy.isExpired(web, LAST_USE.plus(Duration.ofHours(12))))
                .isTrue();
    }

    @Test
    void mobileSessionLivesThirtyDaysAfterLastUse() {
        Session mobile = session(ClientType.MOBILE, LAST_USE);

        assertThat(SessionPolicy.isExpired(
                        mobile, LAST_USE.plus(Duration.ofDays(30).minusSeconds(1))))
                .isFalse();
        assertThat(SessionPolicy.isExpired(mobile, LAST_USE.plus(Duration.ofDays(30))))
                .isTrue();
    }

    @Test
    void useSlidesTheLimit() {
        Session used = session(ClientType.WEB, LAST_USE).rotated("h2", LAST_USE.plus(Duration.ofHours(11)));

        assertThat(SessionPolicy.isExpired(used, LAST_USE.plus(Duration.ofHours(22))))
                .isFalse();
        assertThat(SessionPolicy.isExpired(used, LAST_USE.plus(Duration.ofHours(23))))
                .isTrue();
    }
}
