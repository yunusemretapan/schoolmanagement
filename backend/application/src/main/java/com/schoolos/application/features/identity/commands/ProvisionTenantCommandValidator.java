package com.schoolos.application.features.identity.commands;

import com.schoolos.application.features.identity.rules.IdentityBusinessRules;
import com.schoolos.core.application.behaviors.RequestValidator;
import java.util.Map;

public final class ProvisionTenantCommandValidator implements RequestValidator<ProvisionTenantCommand> {

    @Override
    public Class<ProvisionTenantCommand> requestType() {
        return ProvisionTenantCommand.class;
    }

    @Override
    public Map<String, String> validate(ProvisionTenantCommand request) {
        return IdentityBusinessRules.checkNewAccount(request.email(), request.phone(), request.password());
    }
}
