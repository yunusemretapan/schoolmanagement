package com.schoolos.persistence.config;

import com.schoolos.core.application.behaviors.TenantSessionPort;
import com.schoolos.core.persistence.TenantFilter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Binds the current transaction to one tenant, both halves of BR-1 (ADR 0002): the Hibernate filter for JPQL reads,
 * and the transaction-local {@code app.tenant_id} setting that every Row-Level Security policy reads.
 *
 * <p>The one native statement in the code base, justified here (docs/conventions.md, Known traps): RLS is
 * configured through a PostgreSQL setting, which JPQL cannot express. {@code set_config(..., true)} is local to the
 * transaction, so a pooled connection never carries a tenant into the next request.
 */
@Component
class JpaTenantSession implements TenantSessionPort {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void bind(UUID tenantId) {
        entityManager
                .unwrap(org.hibernate.Session.class)
                .enableFilter(TenantFilter.NAME)
                .setParameter(TenantFilter.PARAMETER, tenantId);
        entityManager
                .createNativeQuery("select set_config('" + TenantFilter.RLS_SETTING + "', :tenant, true)")
                .setParameter("tenant", tenantId.toString())
                .getSingleResult();
    }
}
