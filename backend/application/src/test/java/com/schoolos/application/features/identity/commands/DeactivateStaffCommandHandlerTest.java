package com.schoolos.application.features.identity.commands;

import static com.schoolos.application.features.identity.StaffFixture.roles;
import static org.assertj.core.api.Assertions.assertThat;

import com.schoolos.application.features.identity.StaffFixture;
import com.schoolos.application.features.identity.constants.IdentityErrorCodes;
import com.schoolos.application.features.identity.dtos.InvitationLink;
import com.schoolos.application.features.identity.dtos.StaffDetail;
import com.schoolos.application.features.identity.rules.InvitationPolicy;
import com.schoolos.application.features.identity.rules.SessionActivityChecker;
import com.schoolos.core.crosscutting.exceptions.BusinessException;
import com.schoolos.domain.identity.AccountStatus;
import com.schoolos.domain.identity.AuditAction;
import com.schoolos.domain.identity.ClientType;
import com.schoolos.domain.identity.Role;
import com.schoolos.domain.identity.UserAccount;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Spec 0004, Req. 8, 9 / AC-8, AC-9. */
class DeactivateStaffCommandHandlerTest {

    private final StaffFixture s = new StaffFixture();
    private final SessionActivityChecker checker = new SessionActivityChecker(
            s.fx.sessionRepository(),
            s.fx.identityRepository(),
            s.fx.userRepository(),
            s.fx.transactions,
            s.fx.tenantSession());

    @BeforeEach
    void signIn() {
        s.actAs(s.admin);
    }

    @AfterEach
    void signOut() {
        StaffFixture.signOut();
    }

    private static BusinessException refusal(Runnable action) {
        try {
            action.run();
        } catch (BusinessException e) {
            return e;
        }
        throw new AssertionError("expected a refusal");
    }

    @Test
    void everySessionOnEveryDeviceEndsAtOnce() {
        UUID laptop = s.openSession(s.teacher, ClientType.WEB);
        UUID phone = s.openSession(s.teacher, ClientType.MOBILE);

        StaffDetail detail = s.deactivate.handle(new DeactivateStaffCommand(s.teacher.id()));

        assertThat(detail.status()).isEqualTo(AccountStatus.INACTIVE);
        assertThat(s.fx.sessions.get(laptop).isRevoked()).isTrue();
        assertThat(s.fx.sessions.get(phone).isRevoked()).isTrue();
        assertThat(checker.currentRoles(laptop, s.teacher.tenantId())).isEmpty();
        assertThat(checker.currentRoles(phone, s.teacher.tenantId())).isEmpty();
        assertThat(s.identity(s.teacher).canSignIn()).isFalse();
        assertThat(s.fx.audit).extracting(e -> e.action()).containsExactly(AuditAction.STAFF_DEACTIVATED);
    }

    @Test
    void aPendingInvitationLinkStopsWorking() {
        InvitationLink link =
                s.invite.handle(new InviteStaffCommand("Yeni", "yeni@ornek.test", null, roles(Role.TEACHER)));

        s.deactivate.handle(new DeactivateStaffCommand(link.staffId()));

        UUID invitationId = InvitationPolicy.parse(link.token()).orElseThrow().invitationId();
        assertThat(s.fx.invitations.get(invitationId).isUsable(s.fx.clock.instant()))
                .isFalse();
    }

    @Test
    void anAdministratorCannotDeactivateThemselves() {
        s.seedStaff("İkinci Yönetici", "iki@ornek.test", Role.ADMIN);

        assertThat(refusal(() -> s.deactivate.handle(new DeactivateStaffCommand(s.admin.id())))
                        .code())
                .isEqualTo(IdentityErrorCodes.SELF_CHANGE);
        assertThat(s.identity(s.admin).status()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void theLastActiveAdministratorIsNeverDeactivatedAndTheRuleRunsUnderTheTenantLock() {
        UserAccount second = s.seedStaff("İkinci Yönetici", "iki@ornek.test", Role.ADMIN);
        // Two administrators deactivate each other at once: `second` won, the caller's request is next in line.
        s.fx.identities.put(s.admin.id(), s.identity(s.admin).deactivated());

        assertThat(refusal(() -> s.deactivate.handle(new DeactivateStaffCommand(second.id())))
                        .code())
                .isEqualTo(IdentityErrorCodes.LAST_ADMIN);
        assertThat(s.fx.locks).containsSubsequence("tenant:" + s.admin.tenantId(), "rule:last-admin");
        assertThat(s.identity(second).status()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void withTwoActiveAdministratorsOneMayDeactivateTheOther() {
        UserAccount second = s.seedStaff("İkinci Yönetici", "iki@ornek.test", Role.ADMIN);

        s.deactivate.handle(new DeactivateStaffCommand(second.id()));

        assertThat(s.identity(second).status()).isEqualTo(AccountStatus.INACTIVE);
    }

    @Test
    void anInactiveAccountIsNotDeactivatedTwice() {
        s.deactivate.handle(new DeactivateStaffCommand(s.teacher.id()));

        assertThat(refusal(() -> s.deactivate.handle(new DeactivateStaffCommand(s.teacher.id())))
                        .code())
                .isEqualTo(IdentityErrorCodes.STAFF_STATE);
    }

    @Test
    void theAccountIsLockedAfterTheTenantAndBeforeItsLinks() {
        InvitationLink link =
                s.invite.handle(new InviteStaffCommand("Yeni", "yeni@ornek.test", null, roles(Role.TEACHER)));
        s.fx.locks.clear();

        s.deactivate.handle(new DeactivateStaffCommand(link.staffId()));

        // review 1, M-1: an acceptance of the link at the same moment waits for this change
        assertThat(s.fx.locks)
                .containsSubsequence(
                        "tenant:" + s.admin.tenantId(),
                        "identity:" + link.staffId(),
                        "invitations-of:" + link.staffId());
    }
}
