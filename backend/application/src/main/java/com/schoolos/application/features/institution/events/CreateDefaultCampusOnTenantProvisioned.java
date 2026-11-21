package com.schoolos.application.features.institution.events;

import com.schoolos.application.features.identity.events.TenantProvisioned;
import com.schoolos.application.features.institution.ports.AcademicSettingsRepository;
import com.schoolos.application.features.institution.ports.CampusRepository;
import com.schoolos.core.application.events.DomainEventHandler;
import com.schoolos.core.crosscutting.ids.IdGenerator;
import com.schoolos.domain.institution.AcademicNames;
import com.schoolos.domain.institution.AcademicSettings;
import com.schoolos.domain.institution.Campus;
import java.time.Clock;

/**
 * Spec 0005, Req. 3 / AC-3: a new institution starts with its settings row and one campus named after it (the first
 * 60 characters), in the provisioning transaction with its tenant bound (ADR 0006). Not an administrator's change, so
 * no audit record: the institution's founder has not acted yet.
 */
public final class CreateDefaultCampusOnTenantProvisioned implements DomainEventHandler<TenantProvisioned> {

    private final AcademicSettingsRepository settings;
    private final CampusRepository campuses;
    private final IdGenerator ids;
    private final Clock clock;

    public CreateDefaultCampusOnTenantProvisioned(
            AcademicSettingsRepository settings, CampusRepository campuses, IdGenerator ids, Clock clock) {
        this.settings = settings;
        this.campuses = campuses;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    public Class<TenantProvisioned> eventType() {
        return TenantProvisioned.class;
    }

    @Override
    public void handle(TenantProvisioned event) {
        settings.create(new AcademicSettings(ids.next(), event.tenantId(), null));
        campuses.create(new Campus(ids.next(), event.tenantId(), campusName(event.institutionName()), clock.instant()));
    }

    static String campusName(String institutionName) {
        String name = AcademicNames.normalize(institutionName);
        return name.length() <= AcademicNames.MAX_LENGTH
                ? name
                : name.substring(0, AcademicNames.MAX_LENGTH).strip();
    }
}
