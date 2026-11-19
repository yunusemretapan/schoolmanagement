package com.schoolos.application.features.identity.commands;

import static org.assertj.core.api.Assertions.assertThat;

import com.schoolos.application.features.identity.IdentityFixture;
import com.schoolos.application.features.identity.constants.IdentityErrorCodes;
import com.schoolos.application.features.identity.dtos.AuthOutcome;
import com.schoolos.application.features.identity.rules.RefreshTokens;
import com.schoolos.core.security.AccessClaims;
import com.schoolos.core.security.TokenService;
import com.schoolos.domain.identity.ClientType;
import com.schoolos.domain.identity.Role;
import com.schoolos.domain.identity.Session;
import com.schoolos.domain.identity.UserAccount;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Spec 0003, AC-8 (sliding idle limits) and AC-9 (reuse of an old refresh token revokes the session). */
class RefreshSessionCommandHandlerTest {

    private static final AuthOutcome.Refused EXPIRED = new AuthOutcome.Refused(IdentityErrorCodes.SESSION_EXPIRED);

    private final IdentityFixture fx = new IdentityFixture();

    private final TokenService tokens = new TokenService() {
        @Override
        public String issue(UUID userId, UUID tenantId, UUID sessionId, Set<String> roles) {
            return "access:" + sessionId;
        }

        @Override
        public Optional<AccessClaims> verify(String token) {
            return Optional.empty();
        }
    };

    private final RefreshSessionCommandHandler handler = new RefreshSessionCommandHandler(
            fx.sessionRepository(), fx.userRepository(), tokens, fx.tenantSession(), fx.secrets, fx.clock);

    private final UserAccount guardian =
            fx.seedUser("Örnek Koleji", "Veli Kaya", "veli@ornek.test", null, "Ornek-Sifre-2026", Role.GUARDIAN);

    private String openSession(ClientType type) {
        UUID id = fx.ids.next();
        fx.sessions.put(
                id,
                new Session(
                        id,
                        guardian.id(),
                        guardian.tenantId(),
                        type,
                        RefreshTokens.hash("first"),
                        fx.clock.instant(),
                        fx.clock.instant(),
                        null));
        return RefreshTokens.format(id, "first");
    }

    private AuthOutcome refresh(String token) {
        return handler.handle(new RefreshSessionCommand(token));
    }

    @Test
    void validRefreshRotatesTheSecretAndIssuesANewAccessToken() {
        String first = openSession(ClientType.MOBILE);
        UUID sessionId = RefreshTokens.parse(first).orElseThrow().sessionId();
        fx.clock.advance(Duration.ofMinutes(15));

        AuthOutcome.Issued issued = (AuthOutcome.Issued) refresh(first);

        assertThat(issued.accessToken()).isEqualTo("access:" + sessionId);
        assertThat(issued.refreshToken()).isEqualTo(sessionId + ".secret-1");
        assertThat(issued.clientType()).isEqualTo(ClientType.MOBILE);
        assertThat(fx.sessions.get(sessionId).lastUsedAt()).isEqualTo(fx.clock.instant());
        assertThat(fx.bindings).containsExactly(guardian.tenantId());
    }

    @Test
    void webSessionDropsTwelveHoursAfterLastUseAndUseSlidesIt() {
        String token = openSession(ClientType.WEB);

        fx.clock.advance(Duration.ofHours(11).plusMinutes(59));
        AuthOutcome.Issued slid = (AuthOutcome.Issued) refresh(token);

        fx.clock.advance(Duration.ofHours(11).plusMinutes(59));
        AuthOutcome.Issued slidAgain = (AuthOutcome.Issued) refresh(slid.refreshToken());

        fx.clock.advance(Duration.ofHours(12));
        assertThat(refresh(slidAgain.refreshToken())).isEqualTo(EXPIRED);
    }

    @Test
    void mobileSessionDropsThirtyDaysAfterLastUse() {
        String token = openSession(ClientType.MOBILE);
        fx.clock.advance(Duration.ofDays(30).minusSeconds(1));
        AuthOutcome.Issued stillOpen = (AuthOutcome.Issued) refresh(token);

        fx.clock.advance(Duration.ofDays(30));
        assertThat(refresh(stillOpen.refreshToken())).isEqualTo(EXPIRED);
    }

    @Test
    void reusingAnOldRefreshTokenRevokesTheWholeSessionIncludingTheLatestRotation() {
        String first = openSession(ClientType.WEB);
        AuthOutcome.Issued second = (AuthOutcome.Issued) refresh(first);

        assertThat(refresh(first)).isEqualTo(EXPIRED);

        UUID sessionId = RefreshTokens.parse(first).orElseThrow().sessionId();
        assertThat(fx.sessions.get(sessionId).isRevoked()).isTrue();
        assertThat(refresh(second.refreshToken())).isEqualTo(EXPIRED);
    }

    @Test
    void malformedUnknownAndRevokedTokensAreAllRefusedAlike() {
        assertThat(refresh(null)).isEqualTo(EXPIRED);
        assertThat(refresh("garbage")).isEqualTo(EXPIRED);
        assertThat(refresh(RefreshTokens.format(UUID.randomUUID(), "x"))).isEqualTo(EXPIRED);

        String token = openSession(ClientType.WEB);
        UUID id = RefreshTokens.parse(token).orElseThrow().sessionId();
        fx.sessions.put(id, fx.sessions.get(id).revoked(fx.clock.instant()));
        assertThat(refresh(token)).isEqualTo(EXPIRED);
    }
}
