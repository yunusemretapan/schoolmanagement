package com.schoolos.persistence.identity;

import com.schoolos.domain.identity.LoginIdentity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Global on purpose (plan 0003, R1): the tenant is unknown until the password matched. Identity handlers only. */
@Entity
@Table(name = "login_identities")
class LoginIdentityEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    private String email;

    private String phone;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    protected LoginIdentityEntity() {}

    LoginIdentityEntity(LoginIdentity identity) {
        this.userId = identity.userId();
        this.tenantId = identity.tenantId();
        apply(identity);
    }

    final void apply(LoginIdentity identity) {
        this.email = identity.email();
        this.phone = identity.phone();
        this.passwordHash = identity.passwordHash();
        this.failedAttempts = identity.failedAttempts();
        this.lockedUntil = identity.lockedUntil();
    }

    LoginIdentity toDomain() {
        return new LoginIdentity(userId, tenantId, email, phone, passwordHash, failedAttempts, lockedUntil);
    }
}
