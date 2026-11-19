package com.schoolos.application.features.identity.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.schoolos.application.features.identity.IdentityFixture;
import com.schoolos.application.features.identity.constants.IdentityErrorCodes;
import com.schoolos.application.features.identity.dtos.AuthOutcome;
import com.schoolos.application.features.identity.ports.SourceRateLimiter;
import com.schoolos.application.features.identity.rules.RefreshTokens;
import com.schoolos.core.crosscutting.exceptions.TooManyRequestsException;
import com.schoolos.core.security.AccessClaims;
import com.schoolos.core.security.TokenService;
import com.schoolos.domain.identity.ClientType;
import com.schoolos.domain.identity.Role;
import com.schoolos.domain.identity.Session;
import com.schoolos.domain.identity.UserAccount;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Spec 0003, AC-2, AC-3, AC-4, AC-5. */
class LoginCommandHandlerTest {

    private static final String PASSWORD = "Dogru-Sifre-2026";

    private final IdentityFixture fx = new IdentityFixture();
    private final List<String> issued = new ArrayList<>();
    private boolean rateLimited;

    private final TokenService tokens = new TokenService() {
        @Override
        public String issue(UUID userId, UUID tenantId, UUID sessionId, Set<String> roles) {
            String token = "access:" + userId + ":" + tenantId + ":" + sessionId + ":"
                    + roles.stream().sorted().toList();
            issued.add(token);
            return token;
        }

        @Override
        public Optional<AccessClaims> verify(String token) {
            return Optional.empty();
        }
    };

    private final SourceRateLimiter limiter = source -> !rateLimited;

    private final LoginCommandHandler handler = new LoginCommandHandler(
            fx.identityRepository(),
            fx.userRepository(),
            fx.sessionRepository(),
            limiter,
            fx.passwords,
            tokens,
            fx.tenantSession(),
            fx.secrets,
            fx.ids,
            fx.clock);

    private final UserAccount teacherGuardian = fx.seedUser(
            "Örnek Koleji", "Ayşe Yılmaz", "ayse@ornek.test", "+905321234567", PASSWORD, Role.TEACHER, Role.GUARDIAN);

    private AuthOutcome login(String identifier, String password) {
        return handler.handle(new LoginCommand(identifier, password, ClientType.WEB, "203.0.113.7"));
    }

    @Test
    void rightEmailAndPasswordSignsInWithTenantAndAllRoles() {
        AuthOutcome outcome = login("ayse@ornek.test", PASSWORD);

        assertThat(outcome).isInstanceOf(AuthOutcome.Issued.class);
        AuthOutcome.Issued signedIn = (AuthOutcome.Issued) outcome;
        Session session = fx.sessions.values().iterator().next();
        assertThat(signedIn.accessToken())
                .isEqualTo("access:" + teacherGuardian.id() + ":" + teacherGuardian.tenantId() + ":" + session.id()
                        + ":[GUARDIAN, TEACHER]");
        assertThat(signedIn.accessExpiresInSeconds()).isEqualTo(900);
        assertThat(signedIn.clientType()).isEqualTo(ClientType.WEB);
        assertThat(signedIn.refreshToken()).isEqualTo(session.id() + ".secret-1");
        assertThat(session.tokenHash())
                .isEqualTo(RefreshTokens.hash("secret-1"))
                .doesNotContain("secret-1");
        assertThat(session.userId()).isEqualTo(teacherGuardian.id());
        assertThat(session.clientType()).isEqualTo(ClientType.WEB);
    }

    @Test
    void emailCaseDoesNotMatter() {
        assertThat(login("AYSE@Ornek.TEST", PASSWORD)).isInstanceOf(AuthOutcome.Issued.class);
    }

    @Test
    void everySpellingOfThePhoneNumberSignsIntoTheSameAccount() {
        for (String phone : List.of("05321234567", "5321234567", "+90 532 123 45 67")) {
            assertThat(login(phone, PASSWORD)).as(phone).isInstanceOf(AuthOutcome.Issued.class);
        }
        assertThat(fx.sessions.values()).extracting(Session::userId).containsOnly(teacherGuardian.id());
    }

    @Test
    void tenantIsBoundOnlyAfterThePasswordMatched() {
        login("ayse@ornek.test", "Yanlis-Sifre-2026");
        assertThat(fx.bindings).isEmpty();

        login("ayse@ornek.test", PASSWORD);
        assertThat(fx.bindings).containsExactly(teacherGuardian.tenantId());
    }

    @Test
    void unknownIdentifierWrongPasswordAndLockedAccountGetTheSameRefusal() {
        AuthOutcome unknown = login("kimse@ornek.test", PASSWORD);
        AuthOutcome wrong = login("ayse@ornek.test", "Yanlis-Sifre-2026");
        for (int i = 0; i < 4; i++) {
            login("ayse@ornek.test", "Yanlis-Sifre-2026");
        }
        AuthOutcome locked = login("ayse@ornek.test", PASSWORD);

        AuthOutcome.Refused expected = new AuthOutcome.Refused(IdentityErrorCodes.INVALID_CREDENTIALS);
        assertThat(List.of(unknown, wrong, locked)).containsOnly(expected);
        assertThat(fx.sessions).isEmpty();
    }

    @Test
    void fourFailuresThenTheRightPasswordSignsInAndResetsTheCounter() {
        for (int i = 0; i < 4; i++) {
            login("ayse@ornek.test", "Yanlis-Sifre-2026");
        }
        assertThat(fx.identities.get(teacherGuardian.id()).failedAttempts()).isEqualTo(4);

        assertThat(login("ayse@ornek.test", PASSWORD)).isInstanceOf(AuthOutcome.Issued.class);
        assertThat(fx.identities.get(teacherGuardian.id()).failedAttempts()).isZero();
    }

    @Test
    void fifthFailureLocksEvenTheRightPasswordUntilFifteenMinutesPass() {
        for (int i = 0; i < 5; i++) {
            login("ayse@ornek.test", "Yanlis-Sifre-2026");
        }
        Instant lockedAt = fx.clock.instant();
        assertThat(login("ayse@ornek.test", PASSWORD)).isInstanceOf(AuthOutcome.Refused.class);

        fx.clock.set(lockedAt.plus(Duration.ofMinutes(15)).minusSeconds(1));
        assertThat(login("ayse@ornek.test", PASSWORD)).isInstanceOf(AuthOutcome.Refused.class);

        fx.clock.set(lockedAt.plus(Duration.ofMinutes(15)));
        assertThat(login("ayse@ornek.test", PASSWORD)).isInstanceOf(AuthOutcome.Issued.class);
    }

    @Test
    void sourceOverTheRateLimitIsRefusedBeforeAnyLookup() {
        rateLimited = true;

        assertThatThrownBy(() -> login("ayse@ornek.test", PASSWORD))
                .isInstanceOfSatisfying(
                        TooManyRequestsException.class,
                        e -> assertThat(e.code()).isEqualTo(IdentityErrorCodes.RATE_LIMITED));
        assertThat(fx.identities.get(teacherGuardian.id()).failedAttempts()).isZero();
    }

    @Test
    void validatorRejectsIdentifiersThatAreNeitherEmailNorMobile() {
        LoginCommandValidator validator = new LoginCommandValidator();

        assertThat(validator.validate(new LoginCommand("532123456", PASSWORD, ClientType.WEB, "s")))
                .containsEntry("identifier", IdentityErrorCodes.INVALID_IDENTIFIER);
        assertThat(validator.validate(new LoginCommand("0532abc4567", PASSWORD, ClientType.WEB, "s")))
                .containsEntry("identifier", IdentityErrorCodes.INVALID_IDENTIFIER);
        assertThat(validator.validate(new LoginCommand("ayse@ornek.test", PASSWORD, ClientType.WEB, "s")))
                .isEmpty();
    }

    @Test
    void commandNeverPrintsCredentials() {
        assertThat(new LoginCommand("ayse@ornek.test", PASSWORD, ClientType.MOBILE, "s").toString())
                .doesNotContain("ayse")
                .doesNotContain(PASSWORD);
    }
}
