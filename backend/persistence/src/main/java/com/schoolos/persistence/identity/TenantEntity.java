package com.schoolos.persistence.identity;

import com.schoolos.domain.identity.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** The tenant row itself; Row-Level Security keys on its id (V1__identity.sql). */
@Entity
@Table(name = "tenants")
class TenantEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TenantEntity() {}

    TenantEntity(Tenant tenant) {
        this.id = tenant.id();
        this.name = tenant.name();
        this.createdAt = tenant.createdAt();
    }

    Tenant toDomain() {
        return new Tenant(id, name, createdAt);
    }
}
