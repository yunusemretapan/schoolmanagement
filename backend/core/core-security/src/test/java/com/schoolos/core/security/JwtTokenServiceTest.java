package com.schoolos.core.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtTokenServiceTest {

    private static final byte[] SECRET = "test-secret-test-secret-test-secret!!".getBytes(StandardCharsets.UTF_8);
    private static final Instant NOW = Instant.parse("2026-11-19T10:00:00Z");
    private static final UUID USER = UUID.fromString("01900000-0000-7000-8000-000000000001");
    private static final UUID TENANT = UUID.fromString("01900000-0000-7000-8000-0000000000aa");
    private static final UUID SESSION = UUID.fromString("01900000-0000-7000-8000-0000000000ff");

    private static JwtTokenService at(Instant instant) {
        return new JwtTokenService(SECRET, Clock.fixed(instant, ZoneOffset.UTC));
    }

    @Test
    void roundTripsUserTenantSessionAndRoles() {
        String token = at(NOW).issue(USER, TENANT, SESSION, Set.of("TEACHER", "GUARDIAN"));

        AccessClaims claims = at(NOW).verify(token).orElseThrow();

        assertThat(claims.userId()).isEqualTo(USER);
        assertThat(claims.tenantId()).isEqualTo(TENANT);
        assertThat(claims.sessionId()).isEqualTo(SESSION);
        assertThat(claims.roles()).containsExactlyInAnyOrder("TEACHER", "GUARDIAN");
        assertThat(claims.expiresAt()).isEqualTo(NOW.plus(TokenService.ACCESS_TOKEN_TTL));
    }

    @Test
    void expiresAfterFifteenMinutes() {
        String token = at(NOW).issue(USER, TENANT, SESSION, Set.of("ADMIN"));

        assertThat(at(NOW.plusSeconds(14 * 60 + 59)).verify(token)).isPresent();
        assertThat(at(NOW.plusSeconds(15 * 60)).verify(token)).isEmpty();
    }

    @Test
    void rejectsTamperedTokensAndOtherSecrets() {
        String token = at(NOW).issue(USER, TENANT, SESSION, Set.of("ADMIN"));
        String[] parts = token.split("\\.");
        String otherTenantPayload =
                at(NOW).issue(USER, UUID.randomUUID(), SESSION, Set.of("ADMIN")).split("\\.")[1];

        assertThat(at(NOW).verify(parts[0] + "." + otherTenantPayload + "." + parts[2]))
                .isEmpty();
        assertThat(new JwtTokenService(
                                "another-secret-another-secret-another".getBytes(StandardCharsets.UTF_8),
                                Clock.fixed(NOW, ZoneOffset.UTC))
                        .verify(token))
                .isEmpty();
        assertThat(at(NOW).verify("not-a-jwt")).isEmpty();
    }
}
