package com.schoolos.persistence.institution;

import com.schoolos.core.persistence.BaseTenantEntity;
import com.schoolos.domain.institution.Term;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Tenant-bound: Hibernate tenant filter (inherited) plus Row-Level Security (V4__academic_structure.sql). */
@Entity
@Table(name = "terms")
class TermEntity extends BaseTenantEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "starts_on", nullable = false)
    private LocalDate startsOn;

    @Column(name = "ends_on", nullable = false)
    private LocalDate endsOn;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TermEntity() {}

    TermEntity(Term term) {
        super(term.tenantId());
        this.id = term.id();
        this.createdAt = term.createdAt();
        apply(term);
    }

    final void apply(Term term) {
        this.name = term.name();
        this.startsOn = term.startsOn();
        this.endsOn = term.endsOn();
    }

    Term toDomain() {
        return new Term(id, getTenantId(), name, startsOn, endsOn, createdAt);
    }
}
