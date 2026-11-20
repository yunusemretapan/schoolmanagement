package com.schoolos.application.features.identity.rules;

import com.schoolos.core.security.Permission;
import com.schoolos.domain.identity.Role;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Role → permissions. Spec 0003 grants what every signed-in user needs; module specs add their own (spec 0004:
 * staff management, administrators only).
 * {@link Permission#PLATFORM_OPERATOR} is granted to no role.
 */
public final class RolePermissions {

    private static final Set<Permission> EVERY_USER = EnumSet.of(Permission.SELF_READ);

    private static final Map<Role, Set<Permission>> BY_ROLE = Map.of(
            Role.ADMIN, EnumSet.of(Permission.SELF_READ, Permission.STAFF_MANAGE),
            Role.TEACHER, EVERY_USER,
            Role.GUARDIAN, EVERY_USER,
            Role.ACCOUNTANT, EVERY_USER);

    private RolePermissions() {}

    /** Unknown role names grant nothing. */
    public static Set<Permission> forRoleNames(Collection<String> roleNames) {
        EnumSet<Permission> permissions = EnumSet.noneOf(Permission.class);
        for (String name : roleNames) {
            for (Role role : Role.values()) {
                if (role.name().equals(name)) {
                    permissions.addAll(BY_ROLE.get(role));
                }
            }
        }
        return Set.copyOf(permissions);
    }
}
