package com.schoolos.application.features.institution.ports;

import com.schoolos.domain.institution.AcademicSettings;
import java.util.Optional;

/** The bound institution's settings row (spec 0005, plan R3). Tenant-bound. */
public interface AcademicSettingsRepository {

    /**
     * Reads and locks the bound institution's row until the transaction ends ({@code SELECT … FOR UPDATE}). Every
     * write of the academic structure takes it first, so the rules run one after another per institution.
     */
    Optional<AcademicSettings> lockForBoundTenant();

    Optional<AcademicSettings> findForBoundTenant();

    void create(AcademicSettings settings);

    void update(AcademicSettings settings);
}
