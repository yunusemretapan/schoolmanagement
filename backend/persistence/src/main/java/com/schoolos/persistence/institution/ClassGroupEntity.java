package com.schoolos.persistence.institution;

import com.schoolos.core.persistence.BaseTenantEntity;
import com.schoolos.domain.institution.ClassGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Tenant-bound: Hibernate tenant filter (inherited) plus Row-Level Security. Campus, term and grade level are named
 * together with the tenant (composite foreign keys), so the row cannot point at another institution's records.
 */
@Entity
@Table(name = "class_groups")
class ClassGroupEntity extends BaseTenantEntity {

    @Id
    private UUID id;

    @Column(name = "campus_id", nullable = false, updatable = false)
    private UUID campusId;

    @Column(name = "term_id", nullable = false, updatable = false)
    private UUID termId;

    @Column(name = "grade_level_id", nullable = false)
    private UUID gradeLevelId;

    @Column(nullable = false)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ClassGroupEntity() {}

    ClassGroupEntity(ClassGroup group) {
        super(group.tenantId());
        this.id = group.id();
        this.campusId = group.campusId();
        this.termId = group.termId();
        this.createdAt = group.createdAt();
        apply(group);
    }

    final void apply(ClassGroup group) {
        this.name = group.name();
        this.gradeLevelId = group.gradeLevelId();
    }

    ClassGroup toDomain() {
        return new ClassGroup(id, getTenantId(), campusId, termId, gradeLevelId, name, createdAt);
    }
}
