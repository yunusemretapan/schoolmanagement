package com.schoolos.application.features.identity.commands;

import static com.schoolos.application.features.identity.StaffFixture.roles;
import static org.assertj.core.api.Assertions.assertThat;

import com.schoolos.application.features.identity.constants.IdentityErrorCodes;
import com.schoolos.domain.identity.Role;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Spec 0004, AC-4 (server side of the form's field messages); the same rules guard UpdateStaffCommand. */
class InviteStaffCommandValidatorTest {

    private final InviteStaffCommandValidator invite = new InviteStaffCommandValidator();
    private final UpdateStaffCommandValidator update = new UpdateStaffCommandValidator();

    @Test
    void aValidInvitationHasNoErrors() {
        assertThat(invite.validate(new InviteStaffCommand("Ali", "ali@ornek.test", null, roles(Role.TEACHER))))
                .isEmpty();
        assertThat(invite.validate(new InviteStaffCommand("Ali", null, "05321234567", roles(Role.TEACHER))))
                .isEmpty();
    }

    @Test
    void eMailOrPhoneIsRequiredAndEachMustBeValid() {
        assertThat(invite.validate(new InviteStaffCommand("Ali", " ", null, roles(Role.TEACHER))))
                .containsEntry("email", IdentityErrorCodes.IDENTIFIER_REQUIRED);
        assertThat(invite.validate(new InviteStaffCommand("Ali", "ali@", null, roles(Role.TEACHER))))
                .containsEntry("email", IdentityErrorCodes.INVALID_IDENTIFIER);
        assertThat(invite.validate(new InviteStaffCommand("Ali", null, "0212 123 45 67", roles(Role.TEACHER))))
                .containsEntry("phone", IdentityErrorCodes.INVALID_IDENTIFIER);
    }

    @Test
    void atLeastOneRoleAndNeverVeli() {
        assertThat(invite.validate(new InviteStaffCommand("Ali", "ali@ornek.test", null, Set.of())))
                .containsEntry("roles", IdentityErrorCodes.ROLE_REQUIRED);
        assertThat(invite.validate(new InviteStaffCommand("Ali", "ali@ornek.test", null, null)))
                .containsEntry("roles", IdentityErrorCodes.ROLE_REQUIRED);
        Set<Role> withUnknown = new java.util.HashSet<>(java.util.Arrays.asList(Role.TEACHER, null)); // JSON [.., null]
        assertThat(invite.validate(new InviteStaffCommand("Ali", "ali@ornek.test", null, withUnknown)))
                .containsEntry("roles", IdentityErrorCodes.ROLE_REQUIRED);
        assertThat(invite.validate(
                        new InviteStaffCommand("Ali", "ali@ornek.test", null, roles(Role.TEACHER, Role.GUARDIAN))))
                .containsEntry("roles", IdentityErrorCodes.ROLE_NOT_ASSIGNABLE);
    }

    @Test
    void updatesFollowTheSameFieldRules() {
        assertThat(update.validate(new UpdateStaffCommand(null, "Ali", null, null, Set.of())))
                .containsEntry("email", IdentityErrorCodes.IDENTIFIER_REQUIRED)
                .containsEntry("roles", IdentityErrorCodes.ROLE_REQUIRED);
        assertThat(update.validate(new UpdateStaffCommand(null, "Ali", "a@ornek.test", null, roles(Role.GUARDIAN))))
                .containsEntry("roles", IdentityErrorCodes.ROLE_NOT_ASSIGNABLE);
    }
}
