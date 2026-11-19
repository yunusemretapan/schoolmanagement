package com.schoolos.core.security;

/**
 * What a request may require (docs/security.md, default deny). Roles map to permissions in the identity feature;
 * {@link #PLATFORM_OPERATOR} is never granted through HTTP, only to the operator tool.
 */
public enum Permission {
    /** Read one's own account (name, institution, roles). */
    SELF_READ,
    /** End one's own session on this device. */
    SESSION_END,
    /** Create institutions and their first accounts (operator tool and dev seed only). */
    PLATFORM_OPERATOR
}
