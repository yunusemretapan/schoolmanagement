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
import com.schoolos.domain.identity.AuditAction;
import com.schoolos.domain.identity.AuditEvent;
import com.schoolos.domain.identity.Role;
import com.schoolos.domain.identity.UserAccount;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Spec 0004, Req. 7, 9, 11 / AC-5, AC-7, AC-9. */
class UpdateStaffCommandHandlerTest {

    private final StaffFixture s = new StaffFixture();

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
    void rolesAreAddedAndRemovedAndEachChangeIsAudited() {
        StaffDetail detail = s.update.handle(new UpdateStaffCommand(
                s.teacher.id(), "Öğretmen Ali", "ali@ornek.test", "+905321234567", roles(Role.ACCOUNTANT)));

        assertThat(detail.roles()).containsExactly("ACCOUNTANT");
        assertThat(s.user(s.teacher.id()).roles()).containsExactly(Role.ACCOUNTANT);
        assertThat(s.fx.audit)
                .extracting(AuditEvent::action, AuditEvent::detail)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(AuditAction.ROLE_ADDED, "ACCOUNTANT"),
                        org.assertj.core.groups.Tuple.tuple(AuditAction.ROLE_REMOVED, "TEACHER"));
    }

    @Test
    void removingEveryRoleIsRefusedByValidation() {
        assertThat(new UpdateStaffCommandValidator()
                        .validate(new UpdateStaffCommand(s.teacher.id(), "Ali", "ali@ornek.test", null, roles())))
                .containsEntry("roles", IdentityErrorCodes.ROLE_REQUIRED);
    }

    @Test
    void aVeliRoleTheAccountHoldsIsKept() {
        UserAccount teacherGuardian = s.seedStaff("Ayşe", "ayse@ornek.test", Role.TEACHER, Role.GUARDIAN);

        StaffDetail detail = s.update.handle(
                new UpdateStaffCommand(teacherGuardian.id(), "Ayşe", "ayse@ornek.test", null, roles(Role.ADMIN)));

        assertThat(detail.roles()).containsExactly("ADMIN", "GUARDIAN");
    }

    @Test
    void theNameChangesInAnyStatusAndIsAuditedByFieldNameOnly() {
        s.update.handle(new UpdateStaffCommand(
                s.teacher.id(), "Ali Veli", "ali@ornek.test", "+905321234567", roles(Role.TEACHER)));

        assertThat(s.user(s.teacher.id()).fullName()).isEqualTo("Ali Veli");
        assertThat(s.fx.audit).singleElement().satisfies(e -> {
            assertThat(e.action()).isEqualTo(AuditAction.STAFF_DETAILS_CHANGED);
            assertThat(e.detail()).isEqualTo("fullName");
        });
    }

    @Test
    void anActiveAccountsEmailAndPhoneCannotChange() {
        BusinessException e = refusal(() -> s.update.handle(new UpdateStaffCommand(
                s.teacher.id(), "Öğretmen Ali", "yeni@ornek.test", "+905321234567", roles(Role.TEACHER))));

        assertThat(e.code()).isEqualTo(IdentityErrorCodes.IDENTIFIER_LOCKED);
        assertThat(s.identity(s.teacher).email()).isEqualTo("ali@ornek.test");
    }

    @Test
    void anInvitedAccountsEmailChangesAndItsOldLinkStopsWorking() {
        InvitationLink link =
                s.invite.handle(new InviteStaffCommand("Yeni", "yeni@ornek.test", null, roles(Role.TEACHER)));

        StaffDetail detail = s.update.handle(
                new UpdateStaffCommand(link.staffId(), "Yeni", "dogru@ornek.test", null, roles(Role.TEACHER)));

        assertThat(detail.email()).isEqualTo("dogru@ornek.test");
        assertThat(detail.invitationExpiresAt()).isNull();
        var invitation = s.fx.invitations.get(
                InvitationPolicy.parse(link.token()).orElseThrow().invitationId());
        assertThat(invitation.isUsable(s.fx.clock.instant())).isFalse();
    }

    @Test
    void anInvitedAccountCannotTakeAnotherAccountsEmail() {
        InvitationLink link =
                s.invite.handle(new InviteStaffCommand("Yeni", "yeni@ornek.test", null, roles(Role.TEACHER)));

        BusinessException e = refusal(() -> s.update.handle(
                new UpdateStaffCommand(link.staffId(), "Yeni", "kurucu@baska.test", null, roles(Role.TEACHER))));

        assertThat(e.code()).isEqualTo(IdentityErrorCodes.IDENTIFIER_TAKEN);
    }

    @Test
    void anAdministratorCannotDropTheirOwnAdministratorRole() {
        s.seedStaff("İkinci Yönetici", "iki@ornek.test", Role.ADMIN);

        BusinessException e = refusal(() -> s.update.handle(
                new UpdateStaffCommand(s.admin.id(), "Kurucu Ada", "kurucu@ornek.test", null, roles(Role.TEACHER))));

        assertThat(e.code()).isEqualTo(IdentityErrorCodes.SELF_CHANGE);
    }

    @Test
    void theLastActiveAdministratorKeepsTheRoleAndTheRuleRunsUnderTheTenantLock() {
        UserAccount second = s.seedStaff("İkinci Yönetici", "iki@ornek.test", Role.ADMIN);
        // The caller was deactivated by `second` a moment ago (their request was already past the door): plan 0004, R3.
        s.fx.identities.put(s.admin.id(), s.identity(s.admin).deactivated());

        BusinessException e = refusal(() -> s.update.handle(
                new UpdateStaffCommand(second.id(), "İkinci Yönetici", "iki@ornek.test", null, roles(Role.TEACHER))));

        assertThat(e.code()).isEqualTo(IdentityErrorCodes.LAST_ADMIN);
        assertThat(s.fx.locks).containsSubsequence("tenant:" + s.admin.tenantId(), "rule:last-admin");
        assertThat(s.user(second.id()).roles()).containsExactly(Role.ADMIN);
    }

    @Test
    void withTwoActiveAdministratorsOneMayTakeTheRoleFromTheOther() {
        UserAccount second = s.seedStaff("İkinci Yönetici", "iki@ornek.test", Role.ADMIN);

        s.update.handle(
                new UpdateStaffCommand(second.id(), "İkinci Yönetici", "iki@ornek.test", null, roles(Role.TEACHER)));

        assertThat(s.user(second.id()).roles()).containsExactly(Role.TEACHER);
    }

    @Test
    void anotherInstitutionsStaffIsNotFound() {
        assertThatThrownBy(() -> s.update.handle(
                        new UpdateStaffCommand(s.otherAdmin.id(), "X", "kurucu@baska.test", null, roles(Role.TEACHER))))
                .isInstanceOf(com.schoolos.core.crosscutting.exceptions.NotFoundException.class);
        assertThat(s.user(s.otherAdmin.id()).roles()).containsExactly(Role.ADMIN);
    }

    @Test
    void changingAnInvitedAccountLocksItBeforeItsLinks() {
        InvitationLink link =
                s.invite.handle(new InviteStaffCommand("Yeni", "yeni@ornek.test", null, roles(Role.TEACHER)));
        s.fx.locks.clear();

        s.update.handle(new UpdateStaffCommand(link.staffId(), "Yeni", "dogru@ornek.test", null, roles(Role.TEACHER)));

        // review 1, M-1: an acceptance of the link at the same moment waits for this change
        assertThat(s.fx.locks).containsSubsequence("identity:" + link.staffId(), "invitations-of:" + link.staffId());
    }
}
