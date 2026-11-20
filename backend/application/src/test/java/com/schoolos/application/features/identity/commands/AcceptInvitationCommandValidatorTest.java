package com.schoolos.application.features.identity.commands;

import static org.assertj.core.api.Assertions.assertThat;

import com.schoolos.application.features.identity.constants.IdentityErrorCodes;
import org.junit.jupiter.api.Test;

/** Spec 0004, AC-6: 9 characters are refused, 10 are accepted. */
class AcceptInvitationCommandValidatorTest {

    private final AcceptInvitationCommandValidator validator = new AcceptInvitationCommandValidator();

    @Test
    void tenCharactersAtLeast() {
        assertThat(validator.validate(new AcceptInvitationCommand("t", "123456789", "s")))
                .containsEntry("password", IdentityErrorCodes.PASSWORD_TOO_SHORT);
        assertThat(validator.validate(new AcceptInvitationCommand("t", null, "s")))
                .containsEntry("password", IdentityErrorCodes.PASSWORD_TOO_SHORT);
        assertThat(validator.validate(new AcceptInvitationCommand("t", "1234567890", "s")))
                .isEmpty();
    }
}
