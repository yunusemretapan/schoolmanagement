package com.schoolos.persistence.institution;

import com.schoolos.core.persistence.BaseTenantEntity;
import com.schoolos.domain.institution.Campus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Tenant-bound: Hibernate tenant filter (inherited) plus Row-Level Security (V4__academic_structure.sql). */
@Entity
@Table(name = "campuses")
class CampusEntity extends BaseTenantEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CampusEntity() {}

    CampusEntity(Campus campus) {
        super(campus.tenantId());
        this.id = campus.id();
        this.createdAt = campus.createdAt();
        apply(campus);
    }

    final void apply(Campus campus) {
        this.name = campus.name();
    }

    Campus toDomain() {
        return new Campus(id, getTenantId(), name, createdAt);
    }
}
