package com.schoolos.application.features.identity.ports;

import com.schoolos.domain.identity.Tenant;
import java.util.Optional;
import java.util.UUID;

/** Institutions: only the tenant the transaction is bound to is visible (BR-1). */
public interface TenantRepository {

    Optional<Tenant> findById(UUID tenantId);

    void create(Tenant tenant);
}
