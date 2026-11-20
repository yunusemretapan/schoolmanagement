package com.schoolos.application.features.identity.rules;

import com.schoolos.domain.identity.Invitation;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Invitation links (spec 0004, Req. 5; plan 0004, R4). A link is {@code <invitationId>.<secret>}, the same shape as a
 * refresh token: only the SHA-256 of the secret is stored and it is compared in constant time.
 */
public final class InvitationPolicy {

    public static final Duration VALIDITY = Duration.ofHours(72);

    private InvitationPolicy() {}

    public static Invitation issue(UUID id, UUID userId, UUID tenantId, String secret, Instant now) {
        return new Invitation(id, userId, tenantId, RefreshTokens.hash(secret), now, now.plus(VALIDITY), null, null);
    }

    public static String token(Invitation invitation, String secret) {
        return RefreshTokens.format(invitation.id(), secret);
    }

    public record Parsed(UUID invitationId, String secret) {}

    public static Optional<Parsed> parse(String token) {
        return RefreshTokens.parse(token).map(p -> new Parsed(p.sessionId(), p.secret()));
    }

    /** The invitation the token names, if the token is its current secret and it is still usable at {@code now}. */
    public static boolean accepts(Invitation invitation, String secret, Instant now) {
        return RefreshTokens.matches(secret, invitation.tokenHash()) && invitation.isUsable(now);
    }
}
