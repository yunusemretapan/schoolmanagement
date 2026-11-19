package com.schoolos.application.features.identity.ports;

import com.schoolos.domain.identity.LoginIdentity;
import java.util.Optional;

/** Platform-wide login identities (not tenant-bound: plan 0003, R1). Values are already normalized. */
public interface LoginIdentityRepository {

    Optional<LoginIdentity> findByEmail(String email);

    Optional<LoginIdentity> findByPhone(String phone);

    /**
     * Reads and locks the row until the transaction ends ({@code SELECT … FOR UPDATE}), so parallel login attempts on
     * one account are counted one after another and the lockout limit holds (review 1, M-1).
     */
    Optional<LoginIdentity> lockByEmail(String email);

    Optional<LoginIdentity> lockByPhone(String phone);

    void create(LoginIdentity identity);

    void update(LoginIdentity identity);
}
