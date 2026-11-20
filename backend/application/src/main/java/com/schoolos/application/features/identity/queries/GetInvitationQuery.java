package com.schoolos.application.features.identity.queries;

import com.schoolos.application.features.identity.dtos.InvitationInfo;
import com.schoolos.core.application.mediator.Request;
import com.schoolos.core.security.Public;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Who a link invites, shown before the password is set (spec 0004, Req. 6). Open without a session: the link is
 * the credential (docs/security.md, default deny exceptions). {@code source} is set by the web layer.
 */
@Public
public record GetInvitationQuery(
        @NotBlank @Size(max = 200) String token, @NotBlank String source) implements Request<InvitationInfo> {

    @Override
    public String toString() {
        return "GetInvitationQuery[]"; // the token is a secret
    }
}
