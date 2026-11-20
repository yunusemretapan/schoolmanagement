package com.schoolos.application.features.identity.commands;

import static com.schoolos.application.features.identity.StaffFixture.roles;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.schoolos.application.features.identity.StaffFixture;
import com.schoolos.application.features.identity.dtos.InvitationLink;
import com.schoolos.application.features.identity.queries.GetStaffQuery;
import com.schoolos.core.crosscutting.exceptions.NotFoundException;
import com.schoolos.domain.identity.AccountStatus;
import com.schoolos.domain.identity.AuditAction;
import com.schoolos.domain.identity.AuditEvent;
import com.schoolos.domain.identity.Role;
import com.schoolos.domain.identity.UserAccount;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/** Cross-cutting staff rules: the institution boundary (spec 0004, AC-2) and the audit trail (AC-10). */
class StaffCommandHandlersTest {

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
    void anotherInstitutionsStaffAndAnUnknownIdAreRefusedAlikeAndNothingChanges() throws Throwable {
        UUID foreign = s.otherAdmin.id();
        UUID unknown = UUID.randomUUID();
        UserAccount guardianOnly = s.fx.seedUserIn(
                s.admin.tenantId(), "Veli", "veli@ornek.test", null, StaffFixture.PASSWORD, Role.GUARDIAN);
        for (UUID target : new UUID[] {foreign, unknown, guardianOnly.id()}) {
            for (Executable action : new Executable[] {
                () -> s.get.handle(new GetStaffQuery(target)),
                () -> s.update.handle(new UpdateStaffCommand(target, "X", "x@x.test", null, roles(Role.TEACHER))),
                () -> s.deactivate.handle(new DeactivateStaffCommand(target)),
                () -> s.reactivate.handle(new ReactivateStaffCommand(target)),
                () -> s.renew.handle(new RenewInvitationCommand(target))
            }) {
                assertThatThrownBy(action::execute)
                        .isExactlyInstanceOf(NotFoundException.class)
                        .hasMessage("Staff");
            }
        }
        assertThat(s.identity(s.otherAdmin).status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(s.user(foreign).fullName()).isEqualTo("Başka Kurucu");
        assertThat(s.fx.audit).isEmpty();
    }

    @Test
    void everyStaffChangeLeavesOneRecordWithWhoWhomWhatAndWhenButNoSecrets() {
        InvitationLink link =
                s.invite.handle(new InviteStaffCommand("Yeni", "yeni@ornek.test", null, roles(Role.TEACHER)));
        InvitationLink renewed = s.renew.handle(new RenewInvitationCommand(link.staffId()));
        s.update.handle(new UpdateStaffCommand(
                link.staffId(), "Yeni Ad", "yeni2@ornek.test", null, roles(Role.TEACHER, Role.ACCOUNTANT)));
        s.update.handle(
                new UpdateStaffCommand(link.staffId(), "Yeni Ad", "yeni2@ornek.test", null, roles(Role.ACCOUNTANT)));
        s.deactivate.handle(new DeactivateStaffCommand(link.staffId()));
        s.reactivate.handle(new ReactivateStaffCommand(link.staffId()));

        assertThat(s.fx.audit)
                .extracting(AuditEvent::action)
                .containsExactly(
                        AuditAction.STAFF_INVITED,
                        AuditAction.INVITATION_RENEWED,
                        AuditAction.ROLE_ADDED,
                        AuditAction.STAFF_DETAILS_CHANGED,
                        AuditAction.ROLE_REMOVED,
                        AuditAction.STAFF_DEACTIVATED,
                        AuditAction.STAFF_REACTIVATED);
        assertThat(s.fx.audit).allSatisfy(event -> {
            assertThat(event.actorUserId()).isEqualTo(s.admin.id());
            assertThat(event.targetUserId()).isEqualTo(link.staffId());
            assertThat(event.tenantId()).isEqualTo(s.admin.tenantId());
            assertThat(event.at()).isEqualTo(s.fx.clock.instant());
            assertThat(String.valueOf(event.detail()))
                    .doesNotContain(link.token(), renewed.token(), "yeni", "Yeni", "@");
        });
        assertThat(s.fx.audit.get(3).detail()).isEqualTo("fullName,email");
    }
}
