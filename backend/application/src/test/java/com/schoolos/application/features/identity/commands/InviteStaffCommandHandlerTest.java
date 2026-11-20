package com.schoolos.application.features.identity.commands;

import static com.schoolos.application.features.identity.StaffFixture.roles;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.schoolos.application.features.identity.StaffFixture;
import com.schoolos.application.features.identity.constants.IdentityErrorCodes;
import com.schoolos.application.features.identity.dtos.InvitationLink;
import com.schoolos.application.features.identity.rules.InvitationPolicy;
import com.schoolos.core.crosscutting.exceptions.BusinessException;
import com.schoolos.domain.identity.AccountStatus;
import com.schoolos.domain.identity.AuditAction;
import com.schoolos.domain.identity.Invitation;
import com.schoolos.domain.identity.LoginIdentity;
import com.schoolos.domain.identity.Role;
import com.schoolos.domain.identity.UserAccount;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Spec 0004, Req. 4, 5 / AC-4. */
class InviteStaffCommandHandlerTest {

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
    void createsAnInvitedAccountInTheCallersInstitutionWithAOneTimeLink() {
        InvitationLink link = s.invite.handle(
                new InviteStaffCommand("  Yeni Öğretmen ", "Yeni@Ornek.test", "0532 999 88 77", roles(Role.TEACHER)));

        UserAccount created = s.user(link.staffId());
        LoginIdentity identity = s.identity(created);
        assertThat(created.tenantId()).isEqualTo(s.admin.tenantId());
        assertThat(created.fullName()).isEqualTo("Yeni Öğretmen");
        assertThat(created.roles()).containsExactly(Role.TEACHER);
        assertThat(identity.status()).isEqualTo(AccountStatus.INVITED);
        assertThat(identity.email()).isEqualTo("yeni@ornek.test");
        assertThat(identity.phone()).isEqualTo("+905329998877");
        assertThat(identity.passwordHash()).isNull();

        Invitation invitation = s.fx.invitations.values().iterator().next();
        assertThat(link.expiresAt()).isEqualTo(s.fx.clock.instant().plus(InvitationPolicy.VALIDITY));
        assertThat(link.token()).startsWith(invitation.id() + ".");
        assertThat(invitation.tokenHash()).doesNotContain(link.token()); // only the hash is stored
    }

    @Test
    void severalStaffRolesMayBeGivenAtOnce() {
        InvitationLink link = s.invite.handle(
                new InviteStaffCommand("Müdür Yardımcısı", "md@ornek.test", null, roles(Role.ADMIN, Role.ACCOUNTANT)));

        assertThat(s.user(link.staffId()).roles()).containsExactlyInAnyOrder(Role.ADMIN, Role.ACCOUNTANT);
    }

    @Test
    void anIdentifierInUseHereOrInAnotherInstitutionIsRefusedWithTheSameCode() {
        assertThatThrownBy(() ->
                        s.invite.handle(new InviteStaffCommand("Biri", "ALI@ornek.test", null, roles(Role.TEACHER))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(IdentityErrorCodes.IDENTIFIER_TAKEN);
        assertThatThrownBy(() ->
                        s.invite.handle(new InviteStaffCommand("Biri", "kurucu@baska.test", null, roles(Role.TEACHER))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(IdentityErrorCodes.IDENTIFIER_TAKEN);
        assertThatThrownBy(
                        () -> s.invite.handle(new InviteStaffCommand("Biri", null, "05321234567", roles(Role.TEACHER))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(IdentityErrorCodes.IDENTIFIER_TAKEN);
        assertThat(s.fx.invitations).isEmpty();
    }

    @Test
    void theInvitationIsAuditedWithRolesButNoPersonalDataOrToken() {
        InvitationLink link =
                s.invite.handle(new InviteStaffCommand("Yeni", "yeni@ornek.test", null, roles(Role.TEACHER)));

        assertThat(s.fx.audit).singleElement().satisfies(event -> {
            assertThat(event.action()).isEqualTo(AuditAction.STAFF_INVITED);
            assertThat(event.actorUserId()).isEqualTo(s.admin.id());
            assertThat(event.targetUserId()).isEqualTo(link.staffId());
            assertThat(event.tenantId()).isEqualTo(s.admin.tenantId());
            assertThat(event.detail()).isEqualTo("TEACHER");
            assertThat(event.toString()).doesNotContain(link.token(), "yeni@ornek.test", "Yeni");
        });
    }
}
