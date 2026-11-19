package com.schoolos.application.features.identity.rules;

import com.schoolos.domain.identity.ClientType;
import com.schoolos.domain.identity.Session;
import java.time.Duration;
import java.time.Instant;

/**
 * How long a session stays open after its last use (spec 0003, Req. 6): web 12 hours, mobile 30 days. "Use" is a
 * successful refresh; the access token in between lives 15 minutes (plan 0003, R3).
 */
public final class SessionPolicy {

    public static final Duration WEB_IDLE_LIMIT = Duration.ofHours(12);
    public static final Duration MOBILE_IDLE_LIMIT = Duration.ofDays(30);

    private SessionPolicy() {}

    public static Duration idleLimit(ClientType clientType) {
        return switch (clientType) {
            case WEB -> WEB_IDLE_LIMIT;
            case MOBILE -> MOBILE_IDLE_LIMIT;
        };
    }

    public static boolean isExpired(Session session, Instant now) {
        return !now.isBefore(session.lastUsedAt().plus(idleLimit(session.clientType())));
    }
}
