package com.schoolos.core.application.behaviors;

import java.util.UUID;

/**
 * Binds the current transaction to one tenant: enables the Hibernate tenant filter and sets the Row-Level Security
 * setting (ADR 0002, BR-1). Must be called inside a transaction; the binding ends with it.
 */
@FunctionalInterface
public interface TenantSessionPort {

    void bind(UUID tenantId);
}
