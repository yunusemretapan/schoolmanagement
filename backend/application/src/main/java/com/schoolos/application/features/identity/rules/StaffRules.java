package com.schoolos.application.features.identity.rules;

import com.schoolos.application.features.identity.constants.IdentityErrorCodes;
import com.schoolos.application.features.identity.ports.StaffDirectory;
import com.schoolos.core.crosscutting.exceptions.BusinessException;
import com.schoolos.domain.identity.AccountStatus;
import com.schoolos.domain.identity.LoginIdentity;
import com.schoolos.domain.identity.Role;
import com.schoolos.domain.identity.UserAccount;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Staff-management rules (spec 0004, Req. 4, 7, 9). */
public final class StaffRules {

    private final StaffDirectory directory;

    public StaffRules(StaffDirectory directory) {
        this.directory = directory;
    }

    /** At least one role, staff roles only: Veli comes with student records (spec 0004, Req. 4, 7). */
    public static Map<String, String> checkRoles(Collection<Role> roles) {
        Map<String, String> errors = new HashMap<>();
        if (roles == null || roles.isEmpty() || roles.stream().anyMatch(Objects::isNull)) {
            errors.put("roles", IdentityErrorCodes.ROLE_REQUIRED);
        } else if (roles.stream().anyMatch(role -> !role.isStaff())) {
            errors.put("roles", IdentityErrorCodes.ROLE_NOT_ASSIGNABLE);
        }
        return errors;
    }

    /** An administrator never deactivates themselves or drops their own administrator role (spec 0004, Req. 9). */
    public static void ensureNotSelf(UUID caller, UUID target) {
        if (caller.equals(target)) {
            throw new BusinessException(IdentityErrorCodes.SELF_CHANGE, "Administrators cannot do this to themselves");
        }
    }

    /**
     * Refuses to take away the last active administrator (spec 0004, Req. 9). Callers hold the tenant lock first,
     * so the count cannot change underneath (plan 0004, R3).
     */
    public void ensureAnotherActiveAdminRemains(UserAccount target, LoginIdentity identity) {
        if (isLastActiveAdmin(target, identity)) {
            throw new BusinessException(IdentityErrorCodes.LAST_ADMIN, "The institution needs an active administrator");
        }
    }

    public boolean isLastActiveAdmin(UserAccount target, LoginIdentity identity) {
        boolean activeAdmin = target.roles().contains(Role.ADMIN) && identity.status() == AccountStatus.ACTIVE;
        return activeAdmin && directory.countActiveAdmins() <= 1;
    }
}
