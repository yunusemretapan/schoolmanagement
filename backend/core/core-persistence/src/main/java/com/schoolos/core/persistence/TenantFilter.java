package com.schoolos.core.persistence;

/** Names shared by the Hibernate tenant filter and the Row-Level Security setting (ADR 0002). */
public final class TenantFilter {

    public static final String NAME = "tenantFilter";
    public static final String PARAMETER = "tenantId";
    public static final String COLUMN = "tenant_id";

    /** The transaction-local PostgreSQL setting every RLS policy reads (V1__identity.sql). */
    public static final String RLS_SETTING = "app.tenant_id";

    private TenantFilter() {}
}
