package com.schoolos.application.features.identity.ports;

import java.util.UUID;

/**
 * Serializes staff changes within one institution ({@code SELECT … FOR UPDATE} on the tenant row), so two
 * administrators cannot remove each other at the same moment and leave none (plan 0004, R3).
 */
@FunctionalInterface
public interface TenantLock {

    void lock(UUID tenantId);
}
