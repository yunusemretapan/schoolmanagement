package com.schoolos.application.features.identity.ports;

import com.schoolos.domain.identity.UserAccount;
import java.util.Optional;
import java.util.UUID;

/** Tenant-bound users: only the tenant the transaction is bound to is visible (BR-1). */
public interface UserRepository {

    Optional<UserAccount> findById(UUID userId);

    /**
     * Whether the account is in the bound tenant, without loading it: the tenant check that comes before any lock on
     * a global row (spec 0004 review 2, L2-1).
     */
    boolean exists(UUID userId);

    void create(UserAccount user);

    void update(UserAccount user);
}
