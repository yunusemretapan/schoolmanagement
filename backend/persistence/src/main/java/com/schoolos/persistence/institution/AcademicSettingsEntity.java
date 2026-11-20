package com.schoolos.persistence.institution;

import com.schoolos.core.persistence.BaseTenantEntity;
import com.schoolos.domain.institution.AcademicSettings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** One row per institution (unique tenant_id): the active term, and the row every write locks (plan 0005, R3). */
@Entity
@Table(name = "academic_settings")
class AcademicSettingsEntity extends BaseTenantEntity {

    @Id
    private UUID id;

    @Column(name = "active_term_id")
    private UUID activeTermId;

    protected AcademicSettingsEntity() {}

    AcademicSettingsEntity(AcademicSettings settings) {
        super(settings.tenantId());
        this.id = settings.id();
        apply(settings);
    }

    final void apply(AcademicSettings settings) {
        this.activeTermId = settings.activeTermId();
    }

    AcademicSettings toDomain() {
        return new AcademicSettings(id, getTenantId(), activeTermId);
    }
}
