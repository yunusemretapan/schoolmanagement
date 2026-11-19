package com.schoolos.core.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

/**
 * Every tenant-bound entity extends this: it carries {@code tenant_id} and the Hibernate tenant filter, the
 * application-side half of BR-1 (ADR 0002). The database-side half is Row-Level Security on the same column.
 * The filter does not apply to {@code EntityManager.find} or native SQL: read through JPQL (docs/conventions.md).
 */
@MappedSuperclass
@FilterDef(
        name = TenantFilter.NAME,
        parameters = @ParamDef(name = TenantFilter.PARAMETER, type = UUID.class),
        defaultCondition = TenantFilter.COLUMN + " = :" + TenantFilter.PARAMETER)
@Filter(name = TenantFilter.NAME)
public abstract class BaseTenantEntity {

    @Column(name = TenantFilter.COLUMN, nullable = false, updatable = false)
    private UUID tenantId;

    protected BaseTenantEntity() {}

    protected BaseTenantEntity(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getTenantId() {
        return tenantId;
    }
}
