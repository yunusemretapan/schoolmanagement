package com.schoolos.application.features.identity;

import com.schoolos.application.features.identity.ports.AuditLog;
import com.schoolos.application.features.identity.ports.InvitationRepository;
import com.schoolos.application.features.identity.ports.LoginIdentityRepository;
import com.schoolos.application.features.identity.ports.SecretGenerator;
import com.schoolos.application.features.identity.ports.SessionRepository;
import com.schoolos.application.features.identity.ports.StaffDirectory;
import com.schoolos.application.features.identity.ports.TenantLock;
import com.schoolos.application.features.identity.ports.TenantRepository;
import com.schoolos.application.features.identity.ports.UserRepository;
import com.schoolos.application.features.identity.rules.StaffSearchTerm;
import com.schoolos.core.application.behaviors.TenantSessionPort;
import com.schoolos.core.application.behaviors.TransactionPort;
import com.schoolos.core.application.events.DomainEvent;
import com.schoolos.core.application.events.DomainEventPublisher;
import com.schoolos.core.crosscutting.ids.IdGenerator;
import com.schoolos.core.security.PasswordHasher;
import com.schoolos.domain.identity.AccountStatus;
import com.schoolos.domain.identity.AuditEvent;
import com.schoolos.domain.identity.Invitation;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

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
    public final Map<UUID, Invitation> invitations = new HashMap<>();
    /** The audit log; {@link #auditLog()} only ever appends to it. */
    public final List<AuditEvent> audit = new ArrayList<>();

    public final List<UUID> bindings = new ArrayList<>();
    /** Domain events published, in order (ADR 0006); handlers of other modules are not run here. */
    public final List<DomainEvent> events = new ArrayList<>();
    /** The tenant bound when each event was published (empty when none was). */
    public final List<Optional<UUID>> eventTenants = new ArrayList<>();
    /**
     * Row locks the handlers asked for, in order ("identity:<value or user id>", "session:<id>", "invitation:<id>",
     * "invitations-of:<user id>", "tenant:<id>"), and "rule:last-admin" when the last-admin rule was checked: review 1, M-1; plan 0004, R3.
     */
    public final List<String> locks = new ArrayList<>();

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

    /** Work runs directly, in the caller's thread. */
    public final TransactionPort transactions = new TransactionPort() {
        @Override
        public <T> T inTransaction(Supplier<T> work) {
            return work.get();
        }
    };

    public TenantLock tenantLock() {
        return tenantId -> locks.add("tenant:" + tenantId);
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
            public Optional<LoginIdentity> lockByEmail(String email) {
                locks.add("identity:" + email);
                return findByEmail(email);
            }

            @Override
            public Optional<LoginIdentity> lockByPhone(String phone) {
                locks.add("identity:" + phone);
                return findByPhone(phone);
            }

            @Override
            public Optional<LoginIdentity> findByUserId(UUID userId) {
                return Optional.ofNullable(identities.get(userId));
            }

            @Override
            public Optional<LoginIdentity> lockByUserId(UUID userId) {
                locks.add("identity:" + userId);
                return findByUserId(userId);
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
            public boolean exists(UUID userId) {
                return findById(userId).isPresent();
            }

            @Override
            public void create(UserAccount user) {
                if (!boundTenant().map(user.tenantId()::equals).orElse(false)) {
                    throw new IllegalStateException("RLS WITH CHECK would refuse this insert");
                }
                users.put(user.id(), user);
            }

            @Override
            public void update(UserAccount user) {
                if (findById(user.id()).isEmpty()) {
                    throw new IllegalStateException("RLS would hide this row");
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
            public Optional<Session> lockById(UUID sessionId) {
                locks.add("session:" + sessionId);
                return findById(sessionId);
            }

            @Override
            public void create(Session session) {
                sessions.put(session.id(), session);
            }

            @Override
            public void update(Session session) {
                sessions.put(session.id(), session);
            }

            @Override
            public void revokeAllForUser(UUID userId, Instant now) {
                sessions.replaceAll((id, s) -> s.userId().equals(userId) && !s.isRevoked() ? s.revoked(now) : s);
            }
        };
    }

    public InvitationRepository invitationRepository() {
        return new InvitationRepository() {
            @Override
            public Optional<Invitation> findById(UUID invitationId) {
                return Optional.ofNullable(invitations.get(invitationId));
            }

            @Override
            public Optional<Invitation> lockById(UUID invitationId) {
                locks.add("invitation:" + invitationId);
                return findById(invitationId);
            }

            @Override
            public Optional<Invitation> findUsableForUser(UUID userId, Instant now) {
                return invitations.values().stream()
                        .filter(i -> i.userId().equals(userId) && i.isUsable(now))
                        .max(Comparator.comparing(Invitation::createdAt));
            }

            @Override
            public void create(Invitation invitation) {
                invitations.put(invitation.id(), invitation);
            }

            @Override
            public void update(Invitation invitation) {
                invitations.put(invitation.id(), invitation);
            }

            @Override
            public void revokeOpenForUser(UUID userId, Instant now) {
                locks.add("invitations-of:" + userId);
                invitations.replaceAll((id, i) ->
                        i.userId().equals(userId) && i.usedAt() == null && i.revokedAt() == null ? i.revoked(now) : i);
            }
        };
    }

    /** Append only, and only into the bound tenant (RLS WITH CHECK). */
    public DomainEventPublisher eventPublisher() {
        return event -> {
            events.add(event);
            eventTenants.add(boundTenant());
        };
    }

    public AuditLog auditLog() {
        return event -> {
            if (!boundTenant().map(event.tenantId()::equals).orElse(false)) {
                throw new IllegalStateException("RLS WITH CHECK would refuse this insert");
            }
            audit.add(event);
        };
    }

    /** Same rules as the database adapter: bound tenant only, staff roles only, ordered by name then id. */
    public StaffDirectory staffDirectory() {
        return new StaffDirectory() {
            @Override
            public Page search(Optional<StaffSearchTerm> term, int page, int pageSize) {
                List<Row> all = staffRows()
                        .filter(r -> term.map(t -> t.matches(
                                        r.user().fullName(),
                                        r.identity().email(),
                                        r.identity().phone()))
                                .orElse(true))
                        .sorted(Comparator.comparing((Row r) -> r.user().fullName())
                                .thenComparing(r -> r.user().id()))
                        .toList();
                List<Row> slice = all.stream()
                        .skip((long) page * pageSize)
                        .limit(pageSize)
                        .toList();
                return new Page(slice, all.size());
            }

            @Override
            public long countActiveAdmins() {
                locks.add("rule:last-admin");
                return staffRows()
                        .filter(r -> r.user().roles().contains(Role.ADMIN))
                        .filter(r -> r.identity().status() == AccountStatus.ACTIVE)
                        .count();
            }

            private Stream<Row> staffRows() {
                return users.values().stream()
                        .filter(u -> boundTenant().map(u.tenantId()::equals).orElse(false))
                        .filter(u -> u.roles().stream().anyMatch(Role::isStaff))
                        .map(u -> new Row(u, identities.get(u.id())));
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
