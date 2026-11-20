package com.schoolos.application.features.identity.commands;

import com.schoolos.application.features.identity.dtos.StaffDetail;
import com.schoolos.application.features.identity.rules.EmailNormalizer;
import com.schoolos.application.features.identity.rules.PhoneNumberNormalizer;
import com.schoolos.core.application.mediator.Request;
import com.schoolos.core.security.Permission;
import com.schoolos.core.security.RequiresPermission;
import com.schoolos.domain.identity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

/**
 * Save the staff detail screen (spec 0004, Req. 7, 11): the name, the staff roles, and (only while invited) the
 * e-mail and phone. A Veli role the account already holds is kept; it is not part of {@code roles}.
 */
@RequiresPermission(Permission.STAFF_MANAGE)
public record UpdateStaffCommand(
        @NotNull UUID staffId,
        @NotBlank @Size(max = StaffLimits.FULL_NAME_MAX) String fullName,
        @Size(max = EmailNormalizer.MAX_LENGTH) String email,
        @Size(max = PhoneNumberNormalizer.MAX_LENGTH) String phone,
        Set<Role> roles)
        implements Request<StaffDetail> {

    @Override
    public String toString() {
        return "UpdateStaffCommand[staffId=" + staffId + ", roles=" + roles + "]";
    }
}
