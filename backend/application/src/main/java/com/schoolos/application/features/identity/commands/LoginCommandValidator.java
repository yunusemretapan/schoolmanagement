package com.schoolos.application.features.identity.commands;

import com.schoolos.application.features.identity.constants.IdentityErrorCodes;
import com.schoolos.application.features.identity.rules.LoginIdentifier;
import com.schoolos.core.application.behaviors.RequestValidator;
import java.util.Map;

/** The identifier must be an e-mail or a Turkish mobile number (spec 0003, AC-3). Blank is NotBlank's job. */
public final class LoginCommandValidator implements RequestValidator<LoginCommand> {

    @Override
    public Class<LoginCommand> requestType() {
        return LoginCommand.class;
    }

    @Override
    public Map<String, String> validate(LoginCommand request) {
        if (request.identifier() == null || request.identifier().isBlank()) {
            return Map.of();
        }
        return LoginIdentifier.parse(request.identifier()).isPresent()
                ? Map.of()
                : Map.of("identifier", IdentityErrorCodes.INVALID_IDENTIFIER);
    }
}
