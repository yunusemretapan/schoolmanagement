package com.schoolos.application.features.identity.commands;

import com.schoolos.application.features.identity.dtos.InvitationLink;
import com.schoolos.application.features.identity.rules.EmailNormalizer;
import com.schoolos.application.features.identity.rules.PhoneNumberNormalizer;
import com.schoolos.core.application.mediator.Request;
import com.schoolos.core.security.Permission;
import com.schoolos.core.security.RequiresPermission;
import com.schoolos.domain.identity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

/** Invite a staff member: an invited account and a one-time link (spec 0004, Req. 4, 5). */
@RequiresPermission(Permission.STAFF_MANAGE)
public record InviteStaffCommand(
        @NotBlank @Size(max = StaffLimits.FULL_NAME_MAX) String fullName,
        @Size(max = EmailNormalizer.MAX_LENGTH) String email,
        @Size(max = PhoneNumberNormalizer.MAX_LENGTH) String phone,
        Set<Role> roles)
        implements Request<InvitationLink> {

    @Override
    public String toString() {
        return "InviteStaffCommand[roles=" + roles + "]"; // no personal data in logs
    }
}
