package com.schoolos.domain.identity;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Kullanıcı: a person who signs in, bound to exactly one tenant in Faz 1 (spec 0003, Req. 3). */
public record UserAccount(UUID id, UUID tenantId, String fullName, Set<Role> roles, Instant createdAt) {

    public UserAccount {
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("A user needs at least one role");
        }
        roles = Set.copyOf(roles);
    }

    public UserAccount withFullName(String newFullName) {
        return new UserAccount(id, tenantId, newFullName, roles, createdAt);
    }

    public UserAccount withRoles(Set<Role> newRoles) {
        return new UserAccount(id, tenantId, fullName, newRoles, createdAt);
    }
}
