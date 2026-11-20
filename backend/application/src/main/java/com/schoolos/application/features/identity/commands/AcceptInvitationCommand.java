package com.schoolos.application.features.identity.commands;

import com.schoolos.application.features.identity.dtos.AcceptedInvitation;
import com.schoolos.core.application.mediator.Request;
import com.schoolos.core.security.Public;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Set the password from an invitation link (spec 0004, Req. 6). Open without a session: the link is the credential.
 * The "repeat password" check is the form's job; the server needs one password.
 */
@Public
public record AcceptInvitationCommand(
        @NotBlank @Size(max = 200) String token,
        @Size(max = 128) String password,
        @NotBlank String source) implements Request<AcceptedInvitation> {

    @Override
    public String toString() {
        return "AcceptInvitationCommand[]"; // no token or password in logs
    }
}
