package com.schoolos.application.features.identity.commands;

import com.schoolos.application.features.identity.dtos.ProvisionedAccount;
import com.schoolos.application.features.identity.rules.EmailNormalizer;
import com.schoolos.application.features.identity.rules.PhoneNumberNormalizer;
import com.schoolos.core.application.mediator.Request;
import com.schoolos.core.security.Permission;
import com.schoolos.core.security.RequiresPermission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Operator tool: create an institution and its first founder/administrator account (spec 0003, Req. 1). Never
 * reachable over HTTP: no role grants {@link Permission#PLATFORM_OPERATOR}.
 */
@RequiresPermission(Permission.PLATFORM_OPERATOR)
public record ProvisionTenantCommand(
        @NotBlank String institutionName,
        @NotBlank String founderName,
        @Size(max = EmailNormalizer.MAX_LENGTH) String email,
        @Size(max = PhoneNumberNormalizer.MAX_LENGTH) String phone,
        @Size(max = 128) String password)
        implements Request<ProvisionedAccount> {

    @Override
    public String toString() {
        return "ProvisionTenantCommand[]"; // no personal data or password in logs
    }
}
