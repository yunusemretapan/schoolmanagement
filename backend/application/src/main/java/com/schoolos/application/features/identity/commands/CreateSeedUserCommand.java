package com.schoolos.application.features.identity.commands;

import com.schoolos.core.application.mediator.Request;
import com.schoolos.core.security.Permission;
import com.schoolos.core.security.RequiresPermission;
import com.schoolos.domain.identity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

/**
 * Dev seed only: add a sample user with any Faz 1 roles to an existing institution (spec 0003, Req. 1). Operator
 * permission like provisioning; user management for real institutions is spec 0005.
 */
@RequiresPermission(Permission.PLATFORM_OPERATOR)
public record CreateSeedUserCommand(
        @NotNull UUID tenantId,
        @NotBlank String fullName,
        String email,
        String phone,
        @Size(max = 128) String password,
        @NotEmpty Set<Role> roles)
        implements Request<UUID> {

    @Override
    public String toString() {
        return "CreateSeedUserCommand[tenantId=" + tenantId + "]";
    }
}
