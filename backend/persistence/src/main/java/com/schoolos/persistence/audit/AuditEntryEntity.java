package com.schoolos.persistence.audit;

import com.schoolos.core.application.audit.AuditEntry;
import com.schoolos.core.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Tenant-bound and append-only: no setter, {@code @Immutable}, and a database trigger (V2__staff_management.sql).
 * A user target also fills {@code target_user_id}, which keeps its foreign key to {@code users} (V3__audit_targets.sql).
 */
@Entity
@Immutable
@Table(name = "audit_events")
class AuditEntryEntity extends BaseTenantEntity {

    @Id
    private UUID id;

    @Column(name = "actor_user_id", nullable = false, updatable = false)
    private UUID actorUserId;

    @Column(name = "target_type", nullable = false, updatable = false)
    private String targetType;

    @Column(name = "target_id", updatable = false)
    private UUID targetId;

    @Column(name = "target_user_id", updatable = false)
    private UUID targetUserId;

    @Column(name = "target_name", updatable = false)
    private String targetName;

    @Column(nullable = false, updatable = false)
    private String action;

    @Column(updatable = false)
    private String detail;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected AuditEntryEntity() {}

    AuditEntryEntity(AuditEntry entry) {
        super(entry.tenantId());
        this.id = entry.id();
        this.actorUserId = entry.actorUserId();
        this.targetType = entry.targetType();
        boolean user = AuditEntry.USER.equals(entry.targetType());
        this.targetUserId = user ? entry.targetId() : null;
        this.targetId = user ? null : entry.targetId();
        this.targetName = entry.targetName();
        this.action = entry.action();
        this.detail = entry.detail();
        this.occurredAt = entry.at();
    }
}
