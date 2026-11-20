package com.schoolos.application.features.identity.commands;

import static com.schoolos.application.features.identity.StaffFixture.roles;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.schoolos.application.features.identity.StaffFixture;
import com.schoolos.application.features.identity.constants.IdentityErrorCodes;
import com.schoolos.application.features.identity.dtos.InvitationLink;
import com.schoolos.application.features.identity.rules.InvitationPolicy;
import com.schoolos.core.crosscutting.exceptions.BusinessException;
import com.schoolos.domain.identity.AuditAction;
import com.schoolos.domain.identity.Invitation;
import com.schoolos.domain.identity.Role;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Spec 0004, Req. 5 / AC-5: a renewed link replaces the old one. */
class RenewInvitationCommandHandlerTest {

    private final StaffFixture s = new StaffFixture();
    private InvitationLink first;

    @BeforeEach
    void invite() {
        s.actAs(s.admin);
        first = s.invite.handle(new InviteStaffCommand("Yeni", "yeni@ornek.test", null, roles(Role.TEACHER)));
    }

    @AfterEach
    void signOut() {
        StaffFixture.signOut();
    }

    private Invitation invitationOf(InvitationLink link) {
        UUID id = InvitationPolicy.parse(link.token()).orElseThrow().invitationId();
        return s.fx.invitations.get(id);
    }

    @Test
    void aNewLinkRevokesTheOldOneAndRunsAFresh72Hours() {
        s.fx.clock.advance(Duration.ofHours(70));

        InvitationLink second = s.renew.handle(new RenewInvitationCommand(first.staffId()));

        assertThat(invitationOf(first).isUsable(s.fx.clock.instant())).isFalse();
        assertThat(invitationOf(second).isUsable(s.fx.clock.instant())).isTrue();
        assertThat(second.expiresAt()).isEqualTo(s.fx.clock.instant().plus(InvitationPolicy.VALIDITY));
        assertThat(s.fx.audit).extracting(e -> e.action()).contains(AuditAction.INVITATION_RENEWED);
    }

    @Test
    void anActiveAccountGetsNoLink() {
        assertThatThrownBy(() -> s.renew.handle(new RenewInvitationCommand(s.teacher.id())))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(IdentityErrorCodes.STAFF_STATE);
    }
}
