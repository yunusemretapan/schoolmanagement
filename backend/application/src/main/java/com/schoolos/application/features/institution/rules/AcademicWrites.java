package com.schoolos.application.features.institution.rules;

import com.schoolos.application.features.institution.ports.AcademicSettingsRepository;
import com.schoolos.core.application.audit.AuditEntry;
import com.schoolos.core.application.audit.AuditLog;
import com.schoolos.core.crosscutting.ids.IdGenerator;
import com.schoolos.core.security.CurrentUser;
import com.schoolos.core.security.CurrentUserHolder;
import com.schoolos.core.security.UnauthenticatedException;
import com.schoolos.domain.institution.AcademicAuditAction;
import com.schoolos.domain.institution.AcademicRecordType;
import com.schoolos.domain.institution.AcademicSettings;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * What every write of the academic structure shares (spec 0005): the institution's settings row is locked first, so
 * the rules of one institution run one after another (Req. 8, plan R3); every change leaves an audit record (Req. 10).
 */
public final class AcademicWrites {

    private final AcademicSettingsRepository settings;
    private final AuditLog audit;
    private final IdGenerator ids;
    private final Clock clock;

    public AcademicWrites(AcademicSettingsRepository settings, AuditLog audit, IdGenerator ids, Clock clock) {
        this.settings = settings;
        this.audit = audit;
        this.ids = ids;
        this.clock = clock;
    }

    /** Locks the bound institution's settings row until the transaction ends; call before any rule is checked. */
    public AcademicSettings lock() {
        return settings.lockForBoundTenant()
                .orElseThrow(() -> new IllegalStateException("The institution has no academic settings row"));
    }

    public void activate(AcademicSettings current, UUID termId) {
        settings.update(current.withActiveTerm(termId));
    }

    public UUID newId() {
        return ids.next();
    }

    public Instant now() {
        return clock.instant();
    }

    public static CurrentUser caller() {
        return CurrentUserHolder.current().orElseThrow(() -> new UnauthenticatedException("No user"));
    }

    /** One audit record; {@code changedFields} empty → no detail. */
    public void record(
            AcademicRecordType type, UUID id, String name, AcademicAuditAction action, List<String> changedFields) {
        CurrentUser caller = caller();
        audit.append(new AuditEntry(
                ids.next(),
                caller.tenantId(),
                caller.userId(),
                type.name(),
                id,
                name,
                action.name(),
                changedFields.isEmpty() ? null : String.join(",", changedFields),
                clock.instant()));
    }

    public void record(AcademicRecordType type, UUID id, String name, AcademicAuditAction action) {
        record(type, id, name, action, List.of());
    }
}
