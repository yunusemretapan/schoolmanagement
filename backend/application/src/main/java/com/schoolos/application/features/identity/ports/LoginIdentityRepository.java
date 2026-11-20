package com.schoolos.application.features.identity.ports;

import com.schoolos.domain.identity.LoginIdentity;
import java.util.Optional;
import java.util.UUID;

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

    /** The account's identity in any tenant; callers check the tenant themselves (spec 0004). */
    Optional<LoginIdentity> findByUserId(UUID userId);

    /**
     * Reads the account's identity fresh and locks the row until the transaction ends. Every change of an account's
     * state (staff commands, accepting an invitation) takes this lock first, before any invitation row, so parallel
     * changes run one after another instead of overwriting each other (spec 0004 review 1, M-1).
     */
    Optional<LoginIdentity> lockByUserId(UUID userId);

    void create(LoginIdentity identity);

    void update(LoginIdentity identity);
}
