package com.schoolos.application.features.identity.commands;

import com.schoolos.application.features.identity.rules.IdentityBusinessRules;
import com.schoolos.application.features.identity.rules.StaffRules;
import com.schoolos.core.application.behaviors.RequestValidator;
import java.util.Map;

/** Spec 0004, AC-4: e-mail or phone (at least one, each valid), at least one staff role. Name is NotBlank's job. */
public final class InviteStaffCommandValidator implements RequestValidator<InviteStaffCommand> {

    @Override
    public Class<InviteStaffCommand> requestType() {
        return InviteStaffCommand.class;
    }

    @Override
    public Map<String, String> validate(InviteStaffCommand request) {
        Map<String, String> errors = IdentityBusinessRules.checkIdentifiers(request.email(), request.phone());
        errors.putAll(StaffRules.checkRoles(request.roles()));
        return errors;
    }
}
