package com.schoolos.application.features.identity.commands;

import com.schoolos.application.features.identity.rules.IdentityBusinessRules;
import com.schoolos.core.application.behaviors.RequestValidator;
import java.util.Map;

public final class CreateSeedUserCommandValidator implements RequestValidator<CreateSeedUserCommand> {

    @Override
    public Class<CreateSeedUserCommand> requestType() {
        return CreateSeedUserCommand.class;
    }

    @Override
    public Map<String, String> validate(CreateSeedUserCommand request) {
        return IdentityBusinessRules.checkNewAccount(request.email(), request.phone(), request.password());
    }
}
