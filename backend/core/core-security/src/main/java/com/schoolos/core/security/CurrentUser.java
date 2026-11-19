package com.schoolos.core.security;

import java.util.Set;
import java.util.UUID;

/**
 * The signed-in caller, built only from a verified access token (or by the operator tool). The tenant here is the
 * only tenant a request ever runs in (BR-1, spec 0003 Req. 8).
 */
public record CurrentUser(UUID userId, UUID tenantId, UUID sessionId, Set<String> roles, Set<Permission> permissions) {

    public CurrentUser {
        roles = Set.copyOf(roles);
        permissions = Set.copyOf(permissions);
    }

    /** The operator tool's caller: no tenant, no session, only {@link Permission#PLATFORM_OPERATOR}. */
    public static CurrentUser operator() {
        return new CurrentUser(null, null, null, Set.of(), Set.of(Permission.PLATFORM_OPERATOR));
    }

    public boolean has(Permission permission) {
        return permissions.contains(permission);
    }
}
