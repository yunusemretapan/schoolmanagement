package com.schoolos.application.features.identity.ports;

import com.schoolos.domain.identity.LoginIdentity;
import java.util.Optional;

/** Platform-wide login identities (not tenant-bound: plan 0003, R1). Values are already normalized. */
public interface LoginIdentityRepository {

    Optional<LoginIdentity> findByEmail(String email);

    Optional<LoginIdentity> findByPhone(String phone);

    void create(LoginIdentity identity);

    void update(LoginIdentity identity);
}
