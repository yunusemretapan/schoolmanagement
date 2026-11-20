package com.schoolos.application.features.identity.commands;

import com.schoolos.application.features.identity.constants.IdentityErrorCodes;
import com.schoolos.application.features.identity.rules.IdentityBusinessRules;
import com.schoolos.core.application.behaviors.RequestValidator;
import java.util.Map;

/** Spec 0004, AC-6 server side: at least 10 characters (spec 0003, Req. 1). */
public final class AcceptInvitationCommandValidator implements RequestValidator<AcceptInvitationCommand> {

    @Override
    public Class<AcceptInvitationCommand> requestType() {
        return AcceptInvitationCommand.class;
    }

    @Override
    public Map<String, String> validate(AcceptInvitationCommand request) {
        String password = request.password();
        return password == null || password.length() < IdentityBusinessRules.MIN_PASSWORD_LENGTH
                ? Map.of("password", IdentityErrorCodes.PASSWORD_TOO_SHORT)
                : Map.of();
    }
}
