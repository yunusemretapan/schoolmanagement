package com.schoolos.application.features.identity.commands;

import com.schoolos.application.features.identity.ports.LoginIdentityRepository;
import com.schoolos.application.features.identity.ports.UserRepository;
import com.schoolos.application.features.identity.rules.EmailNormalizer;
import com.schoolos.application.features.identity.rules.IdentityBusinessRules;
import com.schoolos.application.features.identity.rules.PhoneNumberNormalizer;
import com.schoolos.core.crosscutting.ids.IdGenerator;
import com.schoolos.core.security.PasswordHasher;
import com.schoolos.domain.identity.LoginIdentity;
import com.schoolos.domain.identity.Role;
import com.schoolos.domain.identity.UserAccount;
import java.time.Clock;
import java.util.Set;
import java.util.UUID;

/** Creates a user and its login identity in an already-bound tenant. Shared by the operator commands. */
final class AccountFactory {

    private final UserRepository users;
    private final LoginIdentityRepository identities;
    private final IdentityBusinessRules rules;
    private final PasswordHasher passwords;
    private final IdGenerator ids;
    private final Clock clock;

    AccountFactory(
            UserRepository users,
            LoginIdentityRepository identities,
            PasswordHasher passwords,
            IdGenerator ids,
            Clock clock) {
        this.users = users;
        this.identities = identities;
        this.rules = new IdentityBusinessRules(identities);
        this.passwords = passwords;
        this.ids = ids;
        this.clock = clock;
    }

    /** Fails with IDENTITY_IDENTIFIER_TAKEN before writing anything when the e-mail or phone is in use. */
    void ensureFree(String email, String phone) {
        rules.ensureIdentifiersFree(normalizedEmail(email), normalizedPhone(phone));
    }

    UUID create(UUID tenantId, String fullName, String email, String phone, String password, Set<Role> roles) {
        UUID userId = ids.next();
        users.create(new UserAccount(userId, tenantId, fullName.strip(), roles, clock.instant()));
        identities.create(new LoginIdentity(
                userId, tenantId, normalizedEmail(email), normalizedPhone(phone), passwords.hash(password), 0, null));
        return userId;
    }

    private static String normalizedEmail(String email) {
        return email == null || email.isBlank()
                ? null
                : EmailNormalizer.normalize(email).orElseThrow();
    }

    private static String normalizedPhone(String phone) {
        return phone == null || phone.isBlank()
                ? null
                : PhoneNumberNormalizer.normalize(phone).orElseThrow();
    }
}
