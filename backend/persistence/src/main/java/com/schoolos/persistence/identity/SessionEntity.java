package com.schoolos.persistence.identity;

import com.schoolos.domain.identity.ClientType;
import com.schoolos.domain.identity.Session;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Global on purpose (plan 0003, R1): checked on every request before a tenant is bound. */
@Entity
@Table(name = "sessions")
class SessionEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", nullable = false, updatable = false)
    private ClientType clientType;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected SessionEntity() {}

    SessionEntity(Session session) {
        this.id = session.id();
        this.userId = session.userId();
        this.tenantId = session.tenantId();
        this.clientType = session.clientType();
        this.createdAt = session.createdAt();
        apply(session);
    }

    final void apply(Session session) {
        this.tokenHash = session.tokenHash();
        this.lastUsedAt = session.lastUsedAt();
        this.revokedAt = session.revokedAt();
    }

    Session toDomain() {
        return new Session(id, userId, tenantId, clientType, tokenHash, createdAt, lastUsedAt, revokedAt);
    }
}
