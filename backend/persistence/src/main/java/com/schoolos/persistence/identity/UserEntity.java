package com.schoolos.persistence.identity;

import com.schoolos.core.persistence.BaseTenantEntity;
import com.schoolos.domain.identity.Role;
import com.schoolos.domain.identity.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Tenant-bound: Hibernate tenant filter (inherited) plus Row-Level Security. */
@Entity
@Table(name = "users")
class UserEntity extends BaseTenantEntity {

    @Id
    private UUID id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "roles", nullable = false, columnDefinition = "text[]")
    private String[] roles;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserEntity() {}

    UserEntity(UserAccount user) {
        super(user.tenantId());
        this.id = user.id();
        this.createdAt = user.createdAt();
        apply(user);
    }

    final void apply(UserAccount user) {
        this.fullName = user.fullName();
        this.roles = user.roles().stream().map(Role::name).sorted().toArray(String[]::new);
    }

    UserAccount toDomain() {
        return new UserAccount(
                id,
                getTenantId(),
                fullName,
                Arrays.stream(roles).map(Role::valueOf).collect(Collectors.toUnmodifiableSet()),
                createdAt);
    }
}
