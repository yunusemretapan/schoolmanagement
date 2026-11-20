package com.schoolos.application.features.identity.commands;

import com.schoolos.application.features.identity.rules.IdentityBusinessRules;
import com.schoolos.application.features.identity.rules.StaffRules;
import com.schoolos.core.application.behaviors.RequestValidator;
import java.util.Map;

/** The same field rules as an invitation (spec 0004, AC-4, AC-7). */
public final class UpdateStaffCommandValidator implements RequestValidator<UpdateStaffCommand> {

    @Override
    public Class<UpdateStaffCommand> requestType() {
        return UpdateStaffCommand.class;
    }

    @Override
    public Map<String, String> validate(UpdateStaffCommand request) {
        Map<String, String> errors = IdentityBusinessRules.checkIdentifiers(request.email(), request.phone());
        errors.putAll(StaffRules.checkRoles(request.roles()));
        return errors;
    }
}
