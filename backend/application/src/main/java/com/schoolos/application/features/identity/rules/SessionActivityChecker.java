package com.schoolos.application.features.identity.rules;

import com.schoolos.application.features.identity.ports.LoginIdentityRepository;
import com.schoolos.application.features.identity.ports.SessionRepository;
import com.schoolos.application.features.identity.ports.UserRepository;
import com.schoolos.core.application.behaviors.TenantSessionPort;
import com.schoolos.core.application.behaviors.TransactionPort;
import com.schoolos.core.security.SessionValidator;
import com.schoolos.domain.identity.LoginIdentity;
import com.schoolos.domain.identity.Role;
import com.schoolos.domain.identity.Session;
import com.schoolos.domain.identity.UserAccount;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * An access token only counts while its session is open (spec 0003, AC-9) and its account is active (spec 0004,
 * Req. 8). The roles it grants are the account's current ones, read here on every request in one short transaction
 * bound to the token's tenant (plan 0004, R2), never the roles frozen in the token.
 */
public final class SessionActivityChecker implements SessionValidator {

    private final SessionRepository sessions;
    private final LoginIdentityRepository identities;
    private final UserRepository users;
    private final TransactionPort transactions;
    private final TenantSessionPort tenantSession;

    public SessionActivityChecker(
            SessionRepository sessions,
            LoginIdentityRepository identities,
            UserRepository users,
            TransactionPort transactions,
            TenantSessionPort tenantSession) {
        this.sessions = sessions;
        this.identities = identities;
        this.users = users;
        this.transactions = transactions;
        this.tenantSession = tenantSession;
    }

    @Override
    public Optional<Set<String>> currentRoles(UUID sessionId, UUID tenantId) {
        return transactions.inTransaction(() -> {
            Optional<Session> session = sessions.findById(sessionId)
                    .filter(s -> !s.isRevoked())
                    .filter(s -> s.tenantId().equals(tenantId));
            if (session.isEmpty()) {
                return Optional.empty();
            }
            UUID userId = session.get().userId();
            if (!identities.findByUserId(userId).map(LoginIdentity::canSignIn).orElse(false)) {
                return Optional.empty();
            }
            tenantSession.bind(tenantId);
            return users.findById(userId).map(SessionActivityChecker::roleNames);
        });
    }

    private static Set<String> roleNames(UserAccount user) {
        return user.roles().stream().map(Role::name).collect(Collectors.toUnmodifiableSet());
    }
}
