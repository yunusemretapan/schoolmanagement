package com.schoolos.persistence.identity;

import com.schoolos.core.persistence.BaseTenantEntity;
import com.schoolos.domain.identity.AuditAction;
import com.schoolos.domain.identity.AuditEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/** Tenant-bound and append-only: no setter, {@code @Immutable}, and a database trigger (V2__staff_management.sql). */
@Entity
@Immutable
@Table(name = "audit_events")
class AuditEventEntity extends BaseTenantEntity {

    @Id
    private UUID id;

    @Column(name = "actor_user_id", nullable = false, updatable = false)
    private UUID actorUserId;

    @Column(name = "target_user_id", nullable = false, updatable = false)
    private UUID targetUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private AuditAction action;

    @Column(updatable = false)
    private String detail;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected AuditEventEntity() {}

    AuditEventEntity(AuditEvent event) {
        super(event.tenantId());
        this.id = event.id();
        this.actorUserId = event.actorUserId();
        this.targetUserId = event.targetUserId();
        this.action = event.action();
        this.detail = event.detail();
        this.occurredAt = event.at();
    }
}
