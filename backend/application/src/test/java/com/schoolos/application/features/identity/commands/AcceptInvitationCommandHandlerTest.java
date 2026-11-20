package com.schoolos.application.features.identity.commands;

import static com.schoolos.application.features.identity.StaffFixture.roles;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.schoolos.application.features.identity.StaffFixture;
import com.schoolos.application.features.identity.constants.IdentityErrorCodes;
import com.schoolos.application.features.identity.dtos.AcceptedInvitation;
import com.schoolos.application.features.identity.dtos.InvitationInfo;
import com.schoolos.application.features.identity.dtos.InvitationLink;
import com.schoolos.application.features.identity.queries.GetInvitationQuery;
import com.schoolos.application.features.identity.queries.GetInvitationQueryHandler;
import com.schoolos.application.features.identity.rules.InvitationGate;
import com.schoolos.application.features.identity.rules.InvitationPolicy;
import com.schoolos.core.crosscutting.exceptions.NotFoundException;
import com.schoolos.core.crosscutting.exceptions.TooManyRequestsException;
import com.schoolos.domain.identity.AccountStatus;
import com.schoolos.domain.identity.AuditAction;
import com.schoolos.domain.identity.Role;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/** Spec 0004, Req. 5, 6 / AC-5, AC-6: reading and accepting a link, without a session. */
class AcceptInvitationCommandHandlerTest {

    private static final String SOURCE = "203.0.113.9";
    private static final String NEW_PASSWORD = "Yeni-Sifre-2026";

    private final StaffFixture s = new StaffFixture();
    private final List<String> limiterKeys = new ArrayList<>();
    private int allowed = Integer.MAX_VALUE;
    private final InvitationGate gate =
            new InvitationGate(s.fx.invitationRepository(), s.fx.identityRepository(), source -> {
                limiterKeys.add(source);
                return limiterKeys.size() <= allowed;
            });
    private final GetInvitationQueryHandler lookup = new GetInvitationQueryHandler(
            gate, s.fx.userRepository(), s.fx.tenantRepository(), s.fx.tenantSession(), s.fx.clock);
    private final AcceptInvitationCommandHandler accept = new AcceptInvitationCommandHandler(
            gate,
            s.fx.invitationRepository(),
            s.fx.identityRepository(),
            s.fx.auditLog(),
            s.fx.passwords,
            s.fx.tenantSession(),
            s.fx.ids,
            s.fx.clock);

    private InvitationLink link;

    @BeforeEach
    void inviteThenLeave() {
        s.actAs(s.admin);
        link = s.invite.handle(new InviteStaffCommand("Yeni Öğretmen", "yeni@ornek.test", null, roles(Role.TEACHER)));
        StaffFixture.signOut();
        s.fx.bindings.clear(); // the visitor's request starts unbound
    }

    private InvitationInfo look(String token) {
        return lookup.handle(new GetInvitationQuery(token, SOURCE));
    }

    private AcceptedInvitation take(String token) {
        return accept.handle(new AcceptInvitationCommand(token, NEW_PASSWORD, SOURCE));
    }

    @Test
    void aValidLinkShowsTheNameAndInstitutionOnly() {
        InvitationInfo info = look(link.token());

        assertThat(info.fullName()).isEqualTo("Yeni Öğretmen");
        assertThat(info.institutionName()).isEqualTo("Örnek Koleji");
    }

    @Test
    void acceptingActivatesTheAccountWithTheNewPasswordAndNamesTheIdentifierForSignIn() {
        AcceptedInvitation accepted = take(link.token());

        var identity = s.fx.identities.get(link.staffId());
        assertThat(identity.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(s.fx.passwords.matches(NEW_PASSWORD, identity.passwordHash()))
                .isTrue();
        assertThat(accepted.identifier()).isEqualTo("yeni@ornek.test");
        assertThat(s.fx.locks).contains("invitation:" + invitationId());
        assertThat(s.fx.audit).last().satisfies(e -> {
            assertThat(e.action()).isEqualTo(AuditAction.INVITATION_ACCEPTED);
            assertThat(e.actorUserId()).isEqualTo(link.staffId());
            assertThat(e.detail()).isNull();
        });
    }

    @Test
    void aLinkWorksOnce() {
        take(link.token());

        assertThatThrownBy(() -> take(link.token())).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> look(link.token())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void expiredWrongMalformedRenewedAndDeactivatedLinksAllGetTheSameAnswer() {
        String forged = InvitationPolicy.parse(link.token()).orElseThrow().invitationId() + ".guessed";
        String unknown = UUID.randomUUID() + ".x";
        List<Executable> attempts = new ArrayList<>();
        for (String token : new String[] {forged, unknown, "garbage", ""}) {
            attempts.add(() -> look(token));
            attempts.add(() -> take(token));
        }
        for (Executable attempt : attempts) {
            assertThatThrownBy(attempt::execute)
                    .isExactlyInstanceOf(NotFoundException.class)
                    .hasMessage("Invitation");
        }

        s.fx.clock.advance(Duration.ofHours(72));
        assertThatThrownBy(() -> look(link.token())).isExactlyInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> take(link.token())).isExactlyInstanceOf(NotFoundException.class);
        assertThat(s.fx.identities.get(link.staffId()).status()).isEqualTo(AccountStatus.INVITED);
    }

    @Test
    void aLinkOfADeactivatedAccountStopsWorkingEvenIfItWasNotRevoked() {
        s.fx.identities.put(link.staffId(), s.fx.identities.get(link.staffId()).deactivated());

        assertThatThrownBy(() -> look(link.token())).isExactlyInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> take(link.token())).isExactlyInstanceOf(NotFoundException.class);
    }

    @Test
    void theTwentyFirstTryAMinuteFromOneSourceIsRefusedInItsOwnBucket() {
        allowed = 20;
        for (int i = 0; i < 20; i++) {
            look(link.token());
        }

        assertThatThrownBy(() -> look(link.token()))
                .isInstanceOf(TooManyRequestsException.class)
                .extracting(e -> ((TooManyRequestsException) e).code())
                .isEqualTo(IdentityErrorCodes.RATE_LIMITED);
        assertThat(limiterKeys).allMatch(key -> key.equals("invitation:" + SOURCE));
    }

    private UUID invitationId() {
        return InvitationPolicy.parse(link.token()).orElseThrow().invitationId();
    }

    @Test
    void acceptingLocksTheAccountBeforeTheLinkAsEveryStaffCommandDoes() {
        take(link.token());

        // review 1, M-1: the same lock order as the staff commands, so a parallel change waits, never overwrites
        assertThat(s.fx.locks).containsSubsequence("identity:" + link.staffId(), "invitation:" + invitationId());
    }

    @Test
    void readingALinkTakesNoLock() {
        s.fx.locks.clear(); // the invitation itself revoked older links
        look(link.token());

        assertThat(s.fx.locks).isEmpty();
    }
}
