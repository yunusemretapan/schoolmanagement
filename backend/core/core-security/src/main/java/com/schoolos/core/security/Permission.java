package com.schoolos.core.security;

/**
 * What a request may require (docs/security.md, default deny). Roles map to permissions in the identity feature;
 * {@link #PLATFORM_OPERATOR} is never granted through HTTP, only to the operator tool. Sign-out needs no permission: it
 * is {@code @Public} and ends only a session the caller proves (spec 0003, review 1 H-1; review 2 L-5).
 */
public enum Permission {
    /** Read one's own account (name, institution, roles). */
    SELF_READ,
    /** List, invite and change the institution's staff and their roles (spec 0004; Kurucu/Yönetici only). */
    STAFF_MANAGE,
    /** Create institutions and their first accounts (operator tool and dev seed only). */
    PLATFORM_OPERATOR
}
