package com.schoolos.application.features.identity.commands;

import static com.schoolos.application.features.identity.StaffFixture.roles;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.schoolos.application.features.identity.StaffFixture;
import com.schoolos.application.features.identity.constants.IdentityErrorCodes;
import com.schoolos.application.features.identity.dtos.InvitationLink;
import com.schoolos.application.features.identity.dtos.StaffDetail;
import com.schoolos.application.features.identity.rules.InvitationPolicy;
import com.schoolos.core.crosscutting.exceptions.BusinessException;
import com.schoolos.domain.identity.AccountStatus;
import com.schoolos.domain.identity.AuditAction;
import com.schoolos.domain.identity.Role;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Spec 0004, Req. 8 (G5) / AC-8. */
class ReactivateStaffCommandHandlerTest {

    private final StaffFixture s = new StaffFixture();

    @BeforeEach
    void signIn() {
        s.actAs(s.admin);
    }

    @AfterEach
    void signOut() {
        StaffFixture.signOut();
    }

    @Test
    void anAccountThatWasActiveSignsInWithItsOldPasswordAgain() {
        s.deactivate.handle(new DeactivateStaffCommand(s.teacher.id()));

        StaffDetail detail = s.reactivate.handle(new ReactivateStaffCommand(s.teacher.id()));

        assertThat(detail.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(s.identity(s.teacher).canSignIn()).isTrue();
        assertThat(s.fx.passwords.matches(
                        StaffFixture.PASSWORD, s.identity(s.teacher).passwordHash()))
                .isTrue();
        assertThat(s.fx.audit)
                .extracting(e -> e.action())
                .containsExactly(AuditAction.STAFF_DEACTIVATED, AuditAction.STAFF_REACTIVATED);
    }

    @Test
    void anAccountDeactivatedBeforeAcceptingIsInvitedAgainAndItsOldLinkStaysDead() {
        InvitationLink link =
                s.invite.handle(new InviteStaffCommand("Yeni", "yeni@ornek.test", null, roles(Role.TEACHER)));
        s.deactivate.handle(new DeactivateStaffCommand(link.staffId()));

        StaffDetail detail = s.reactivate.handle(new ReactivateStaffCommand(link.staffId()));

        assertThat(detail.status()).isEqualTo(AccountStatus.INVITED);
        assertThat(detail.invitationExpiresAt()).isNull();
        UUID invitationId = InvitationPolicy.parse(link.token()).orElseThrow().invitationId();
        assertThat(s.fx.invitations.get(invitationId).isUsable(s.fx.clock.instant()))
                .isFalse();
    }

    @Test
    void onlyAnInactiveAccountIsReactivated() {
        assertThatThrownBy(() -> s.reactivate.handle(new ReactivateStaffCommand(s.teacher.id())))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(IdentityErrorCodes.STAFF_STATE);
    }
}
