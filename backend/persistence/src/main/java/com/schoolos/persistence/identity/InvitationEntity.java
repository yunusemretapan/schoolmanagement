package com.schoolos.persistence.identity;

import com.schoolos.domain.identity.Invitation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Global on purpose (plan 0004, R4): a link is opened before anyone is signed in. Identity handlers only. */
@Entity
@Table(name = "invitations")
class InvitationEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "token_hash", nullable = false, updatable = false)
    private String tokenHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected InvitationEntity() {}

    InvitationEntity(Invitation invitation) {
        this.id = invitation.id();
        this.userId = invitation.userId();
        this.tenantId = invitation.tenantId();
        this.tokenHash = invitation.tokenHash();
        this.createdAt = invitation.createdAt();
        this.expiresAt = invitation.expiresAt();
        apply(invitation);
    }

    final void apply(Invitation invitation) {
        this.usedAt = invitation.usedAt();
        this.revokedAt = invitation.revokedAt();
    }

    Invitation toDomain() {
        return new Invitation(id, userId, tenantId, tokenHash, createdAt, expiresAt, usedAt, revokedAt);
    }
}
