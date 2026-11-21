package com.schoolos.persistence.institution;

import com.schoolos.core.persistence.BaseTenantEntity;
import com.schoolos.domain.institution.GradeLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Tenant-bound: Hibernate tenant filter (inherited) plus Row-Level Security. The position is unique per institution,
 * checked at commit (deferred), so a move can swap two positions (V4__academic_structure.sql).
 */
@Entity
@Table(name = "grade_levels")
class GradeLevelEntity extends BaseTenantEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int position;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected GradeLevelEntity() {}

    GradeLevelEntity(GradeLevel level) {
        super(level.tenantId());
        this.id = level.id();
        this.createdAt = level.createdAt();
        apply(level);
    }

    final void apply(GradeLevel level) {
        this.name = level.name();
        this.position = level.position();
    }

    GradeLevel toDomain() {
        return new GradeLevel(id, getTenantId(), name, position, createdAt);
    }
}
