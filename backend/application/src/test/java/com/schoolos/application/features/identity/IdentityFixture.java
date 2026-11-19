package com.schoolos.application.features.identity;

import com.schoolos.application.features.identity.ports.LoginIdentityRepository;
import com.schoolos.application.features.identity.ports.SecretGenerator;
import com.schoolos.application.features.identity.ports.SessionRepository;
import com.schoolos.application.features.identity.ports.TenantRepository;
import com.schoolos.application.features.identity.ports.UserRepository;
import com.schoolos.core.application.behaviors.TenantSessionPort;
import com.schoolos.core.crosscutting.ids.IdGenerator;
import com.schoolos.core.security.PasswordHasher;
import com.schoolos.domain.identity.LoginIdentity;
import com.schoolos.domain.identity.Role;
import com.schoolos.domain.identity.Session;
import com.schoolos.domain.identity.Tenant;
import com.schoolos.domain.identity.UserAccount;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory ports for the identity feature. Tenant-bound reads (users, tenants) behave like the Hibernate filter and
 * Row-Level Security: only the tenant bound through {@link #tenantSession()} is visible, nothing when unbound.
 */
public final class IdentityFixture {

    public static final Instant START = Instant.parse("2026-11-19T07:00:00Z");

    public final MutableClock clock = new MutableClock(START);
    public final Map<UUID, LoginIdentity> identities = new HashMap<>();
    public final Map<UUID, UserAccount> users = new HashMap<>();
    public final Map<UUID, Tenant> tenants = new HashMap<>();
    public final Map<UUID, Session> sessions = new HashMap<>();
    public final List<UUID> bindings = new ArrayList<>();

    private final AtomicInteger idCounter = new AtomicInteger();
    private final AtomicInteger secretCounter = new AtomicInteger();

    /** Plain, fast, and obviously not a real hash: Argon2id itself is tested in core-security. */
    public final PasswordHasher passwords = new PasswordHasher() {
        @Override
        public String hash(String rawPassword) {
            return "hashed:" + rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String hash) {
            return hash.equals("hashed:" + rawPassword);
        }
    };

    public final IdGenerator ids =
            () -> UUID.fromString(String.format("01900000-0000-7000-8000-%012d", idCounter.incrementAndGet()));

    public final SecretGenerator secrets = () -> "secret-" + secretCounter.incrementAndGet();

    public TenantSessionPort tenantSession() {
        return bindings::add;
    }

    public Optional<UUID> boundTenant() {
        return bindings.isEmpty() ? Optional.empty() : Optional.of(bindings.get(bindings.size() - 1));
    }

    public LoginIdentityRepository identityRepository() {
        return new LoginIdentityRepository() {
            @Override
            public Optional<LoginIdentity> findByEmail(String email) {
                return identities.values().stream()
                        .filter(i -> email.equals(i.email()))
                        .findFirst();
            }

            @Override
            public Optional<LoginIdentity> findByPhone(String phone) {
                return identities.values().stream()
                        .filter(i -> phone.equals(i.phone()))
                        .findFirst();
            }

            @Override
            public void create(LoginIdentity identity) {
                identities.put(identity.userId(), identity);
            }

            @Override
            public void update(LoginIdentity identity) {
                identities.put(identity.userId(), identity);
            }
        };
    }

    public UserRepository userRepository() {
        return new UserRepository() {
            @Override
            public Optional<UserAccount> findById(UUID userId) {
                return Optional.ofNullable(users.get(userId))
                        .filter(u -> boundTenant().map(u.tenantId()::equals).orElse(false));
            }

            @Override
            public void create(UserAccount user) {
                if (!boundTenant().map(user.tenantId()::equals).orElse(false)) {
                    throw new IllegalStateException("RLS WITH CHECK would refuse this insert");
                }
                users.put(user.id(), user);
            }
        };
    }

    public TenantRepository tenantRepository() {
        return new TenantRepository() {
            @Override
            public Optional<Tenant> findById(UUID tenantId) {
                return Optional.ofNullable(tenants.get(tenantId))
                        .filter(t -> boundTenant().map(t.id()::equals).orElse(false));
            }

            @Override
            public void create(Tenant tenant) {
                if (!boundTenant().map(tenant.id()::equals).orElse(false)) {
                    throw new IllegalStateException("RLS WITH CHECK would refuse this insert");
                }
                tenants.put(tenant.id(), tenant);
            }
        };
    }

    public SessionRepository sessionRepository() {
        return new SessionRepository() {
            @Override
            public Optional<Session> findById(UUID sessionId) {
                return Optional.ofNullable(sessions.get(sessionId));
            }

            @Override
            public void create(Session session) {
                sessions.put(session.id(), session);
            }

            @Override
            public void update(Session session) {
                sessions.put(session.id(), session);
            }
        };
    }

    /** Seeds a tenant and a user with a login identity, bypassing the commands. */
    public UserAccount seedUser(
            String tenantName, String fullName, String email, String phone, String password, Role... roles) {
        UUID tenantId = ids.next();
        tenants.put(tenantId, new Tenant(tenantId, tenantName, START));
        return seedUserIn(tenantId, fullName, email, phone, password, roles);
    }

    public UserAccount seedUserIn(
            UUID tenantId, String fullName, String email, String phone, String password, Role... roles) {
        UUID userId = ids.next();
        UserAccount user = new UserAccount(userId, tenantId, fullName, Set.of(roles), START);
        users.put(userId, user);
        identities.put(userId, new LoginIdentity(userId, tenantId, email, phone, "hashed:" + password, 0, null));
        return user;
    }

    /** A clock tests can move forward. */
    public static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        public void advance(Duration duration) {
            now = now.plus(duration);
        }

        public void set(Instant instant) {
            now = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
