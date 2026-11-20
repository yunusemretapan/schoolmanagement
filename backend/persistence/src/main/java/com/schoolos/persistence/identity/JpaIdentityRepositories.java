package com.schoolos.persistence.identity;

import com.schoolos.application.features.identity.constants.IdentityErrorCodes;
import com.schoolos.application.features.identity.ports.AuditLog;
import com.schoolos.application.features.identity.ports.InvitationRepository;
import com.schoolos.application.features.identity.ports.LoginIdentityRepository;
import com.schoolos.application.features.identity.ports.SessionRepository;
import com.schoolos.application.features.identity.ports.StaffDirectory;
import com.schoolos.application.features.identity.ports.TenantLock;
import com.schoolos.application.features.identity.ports.TenantRepository;
import com.schoolos.application.features.identity.ports.UserRepository;
import com.schoolos.application.features.identity.rules.StaffSearchTerm;
import com.schoolos.core.crosscutting.exceptions.BusinessException;
import com.schoolos.domain.identity.AccountStatus;
import com.schoolos.domain.identity.AuditEvent;
import com.schoolos.domain.identity.Invitation;
import com.schoolos.domain.identity.LoginIdentity;
import com.schoolos.domain.identity.Session;
import com.schoolos.domain.identity.Tenant;
import com.schoolos.domain.identity.UserAccount;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Repository;

/**
 * Adapters for the identity ports. Tenant-bound reads go through JPQL, never {@code EntityManager.find}: the
 * Hibernate tenant filter does not apply to {@code find} (docs/conventions.md, Known traps). Row-Level Security
 * guards both either way.
 */
final class JpaIdentityRepositories {

    private JpaIdentityRepositories() {}

    @Repository
    static class Tenants implements TenantRepository {

        @PersistenceContext
        private EntityManager em;

        @Override
        public Optional<Tenant> findById(UUID tenantId) {
            return em.createQuery("select t from TenantEntity t where t.id = :id", TenantEntity.class)
                    .setParameter("id", tenantId)
                    .getResultStream()
                    .findFirst()
                    .map(TenantEntity::toDomain);
        }

        @Override
        public void create(Tenant tenant) {
            em.persist(new TenantEntity(tenant));
        }
    }

    @Repository
    static class TenantRows implements TenantLock {

        @PersistenceContext
        private EntityManager em;

        @Override
        public void lock(UUID tenantId) {
            em.createQuery("select t from TenantEntity t where t.id = :id", TenantEntity.class)
                    .setParameter("id", tenantId)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .getResultList();
        }
    }

    @Repository
    static class Users implements UserRepository {

        @PersistenceContext
        private EntityManager em;

        @Override
        public Optional<UserAccount> findById(UUID userId) {
            return entity(userId).map(UserEntity::toDomain);
        }

        @Override
        public boolean exists(UUID userId) {
            // A scalar query: the tenant filter and RLS apply, and no entity is left in the persistence context to go
            // stale before the caller's lock (review 2, L2-1).
            return em.createQuery("select count(u) from UserEntity u where u.id = :id", Long.class)
                            .setParameter("id", userId)
                            .getSingleResult()
                    > 0;
        }

        @Override
        public void create(UserAccount user) {
            em.persist(new UserEntity(user));
        }

        @Override
        public void update(UserAccount user) {
            entity(user.id()).orElseThrow().apply(user);
        }

        private Optional<UserEntity> entity(UUID userId) {
            return em.createQuery("select u from UserEntity u where u.id = :id", UserEntity.class)
                    .setParameter("id", userId)
                    .getResultStream()
                    .findFirst();
        }
    }

    @Repository
    static class LoginIdentities implements LoginIdentityRepository {

        @PersistenceContext
        private EntityManager em;

        @Override
        public Optional<LoginIdentity> findByEmail(String email) {
            return findBy("email", email);
        }

        @Override
        public Optional<LoginIdentity> findByPhone(String phone) {
            return findBy("phone", phone);
        }

        @Override
        public Optional<LoginIdentity> lockByEmail(String email) {
            return findBy("email", email, LockModeType.PESSIMISTIC_WRITE);
        }

        @Override
        public Optional<LoginIdentity> lockByPhone(String phone) {
            return findBy("phone", phone, LockModeType.PESSIMISTIC_WRITE);
        }

        @Override
        public Optional<LoginIdentity> findByUserId(UUID userId) {
            return Optional.ofNullable(em.find(LoginIdentityEntity.class, userId))
                    .map(LoginIdentityEntity::toDomain);
        }

        @Override
        public Optional<LoginIdentity> lockByUserId(UUID userId) {
            return lockFresh(em, LoginIdentityEntity.class, userId).map(LoginIdentityEntity::toDomain);
        }

        private Optional<LoginIdentity> findBy(String field, String value) {
            return findBy(field, value, LockModeType.NONE);
        }

        private Optional<LoginIdentity> findBy(String field, String value, LockModeType lock) {
            return em.createQuery(
                            "select i from LoginIdentityEntity i where i." + field + " = :value",
                            LoginIdentityEntity.class)
                    .setParameter("value", value)
                    .setLockMode(lock)
                    .getResultStream()
                    .findFirst()
                    .map(LoginIdentityEntity::toDomain);
        }

        @Override
        public void create(LoginIdentity identity) {
            em.persist(new LoginIdentityEntity(identity));
            flushIdentifiers();
        }

        @Override
        public void update(LoginIdentity identity) {
            LoginIdentityEntity entity = em.find(LoginIdentityEntity.class, identity.userId());
            LoginIdentity before = entity.toDomain();
            entity.apply(identity);
            if (!Objects.equals(before.email(), identity.email())
                    || !Objects.equals(before.phone(), identity.phone())) {
                flushIdentifiers();
            }
        }

        /**
         * Writes a new or changed e-mail / phone now, so that the unique index decides a race the business check
         * lost: two invitations with one identifier at the same moment end in IDENTITY_IDENTIFIER_TAKEN (422), not
         * in a 500 at commit (spec 0004 review 1, L-4). Postgres names the indexes {@code <table>_<column>_key}.
         */
        private void flushIdentifiers() {
            try {
                em.flush();
            } catch (PersistenceException e) {
                if (violatesIdentifierIndex(e)) {
                    throw new BusinessException(IdentityErrorCodes.IDENTIFIER_TAKEN, "Identifier already in use");
                }
                throw e;
            }
        }

        private static boolean violatesIdentifierIndex(Throwable e) {
            for (Throwable t = e; t != null; t = t.getCause()) {
                if (t instanceof ConstraintViolationException violation) {
                    return IDENTIFIER_INDEXES.contains(violation.getConstraintName());
                }
            }
            return false;
        }

        private static final Set<String> IDENTIFIER_INDEXES =
                Set.of("login_identities_email_key", "login_identities_phone_key");
    }

    @Repository
    static class Sessions implements SessionRepository {

        @PersistenceContext
        private EntityManager em;

        @Override
        public Optional<Session> findById(UUID sessionId) {
            return Optional.ofNullable(em.find(SessionEntity.class, sessionId)).map(SessionEntity::toDomain);
        }

        @Override
        public Optional<Session> lockById(UUID sessionId) {
            return Optional.ofNullable(em.find(SessionEntity.class, sessionId, LockModeType.PESSIMISTIC_WRITE))
                    .map(SessionEntity::toDomain);
        }

        @Override
        public void create(Session session) {
            em.persist(new SessionEntity(session));
        }

        @Override
        public void update(Session session) {
            SessionEntity entity = em.find(SessionEntity.class, session.id());
            entity.apply(session);
        }

        @Override
        public void revokeAllForUser(UUID userId, Instant now) {
            em.createQuery(
                            "select s from SessionEntity s where s.userId = :user and s.revokedAt is null",
                            SessionEntity.class)
                    .setParameter("user", userId)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .getResultList()
                    .forEach(entity -> entity.apply(entity.toDomain().revoked(now)));
        }
    }

    @Repository
    static class Invitations implements InvitationRepository {

        @PersistenceContext
        private EntityManager em;

        @Override
        public Optional<Invitation> findById(UUID invitationId) {
            return Optional.ofNullable(em.find(InvitationEntity.class, invitationId))
                    .map(InvitationEntity::toDomain);
        }

        @Override
        public Optional<Invitation> lockById(UUID invitationId) {
            return lockFresh(em, InvitationEntity.class, invitationId).map(InvitationEntity::toDomain);
        }

        @Override
        public Optional<Invitation> findUsableForUser(UUID userId, Instant now) {
            return open(userId).stream()
                    .map(InvitationEntity::toDomain)
                    .filter(invitation -> invitation.isUsable(now))
                    .findFirst();
        }

        @Override
        public void create(Invitation invitation) {
            em.persist(new InvitationEntity(invitation));
        }

        @Override
        public void update(Invitation invitation) {
            em.find(InvitationEntity.class, invitation.id()).apply(invitation);
        }

        @Override
        public void revokeOpenForUser(UUID userId, Instant now) {
            open(userId, LockModeType.PESSIMISTIC_WRITE)
                    .forEach(entity -> entity.apply(entity.toDomain().revoked(now)));
        }

        private List<InvitationEntity> open(UUID userId) {
            return open(userId, LockModeType.NONE);
        }

        private List<InvitationEntity> open(UUID userId, LockModeType lock) {
            return em.createQuery(
                            "select i from InvitationEntity i where i.userId = :user and i.usedAt is null"
                                    + " and i.revokedAt is null order by i.createdAt desc",
                            InvitationEntity.class)
                    .setParameter("user", userId)
                    .setLockMode(lock)
                    .getResultList();
        }
    }

    @Repository
    static class Audit implements AuditLog {

        @PersistenceContext
        private EntityManager em;

        @Override
        public void append(AuditEvent event) {
            em.persist(new AuditEventEntity(event));
        }
    }

    /**
     * Staff of the bound tenant: users (tenant filter + RLS) joined with their login identity. JPQL only, so the
     * tenant filter applies (docs/conventions.md, Known traps).
     */
    @Repository
    static class Staff implements StaffDirectory {

        /**
         * Hibernate's {@code array_position}, which answers 0 (not null) when the role is absent. Not
         * {@code array_contains}: it binds {@code varchar[]}, and PostgreSQL has no {@code text[] @> varchar[]}
         * operator. Both found against PostgreSQL in the plan 0004 step 5 smoke run; unit tests cannot see them.
         */
        private static final String HAS_ROLE = "array_position(u.roles, '%s') > 0";

        private static final String FROM = " from UserEntity u join LoginIdentityEntity i on i.userId = u.id"
                + " where i.tenantId = u.tenantId and (" + HAS_ROLE.formatted("ADMIN") + " or "
                + HAS_ROLE.formatted("TEACHER") + " or " + HAS_ROLE.formatted("ACCOUNTANT") + ")";
        /** {@link StaffSearchTerm#fold} in JPQL: the Turkish I letters become {@code i} before {@code lower}. */
        private static final String FOLDED_NAME =
                "lower(replace(replace(replace(u.fullName, 'İ', 'i'), 'I', 'i'), 'ı', 'i'))";

        private static final String SEARCH = " and (" + FOLDED_NAME + " like :text escape '\\'"
                + " or i.email like :text escape '\\' or i.phone like :digits)";
        /** Matches no stored phone: every stored number starts with {@code +}. */
        private static final String NO_PHONE = "-";

        @PersistenceContext
        private EntityManager em;

        @Override
        public Page search(Optional<StaffSearchTerm> term, int page, int pageSize) {
            String where = term.isPresent() ? FROM + SEARCH : FROM;
            TypedQuery<Long> count = em.createQuery("select count(u)" + where, Long.class);
            TypedQuery<Object[]> rows =
                    em.createQuery("select u, i" + where + " order by u.fullName, u.id", Object[].class);
            term.ifPresent(t -> {
                String text = "%" + escape(t.text()) + "%";
                String digits = t.phoneDigits() == null ? NO_PHONE : "%" + t.phoneDigits() + "%";
                count.setParameter("text", text).setParameter("digits", digits);
                rows.setParameter("text", text).setParameter("digits", digits);
            });
            List<Row> found = rows.setFirstResult(page * pageSize).setMaxResults(pageSize).getResultList().stream()
                    .map(r -> new Row(((UserEntity) r[0]).toDomain(), ((LoginIdentityEntity) r[1]).toDomain()))
                    .toList();
            return new Page(found, count.getSingleResult());
        }

        @Override
        public long countActiveAdmins() {
            return em.createQuery(
                            "select count(u) from UserEntity u join LoginIdentityEntity i on i.userId = u.id"
                                    + " where " + HAS_ROLE.formatted("ADMIN") + " and i.status = :active",
                            Long.class)
                    .setParameter("active", AccountStatus.ACTIVE)
                    .getSingleResult();
        }

        private static String escape(String text) {
            return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        }
    }

    /**
     * {@code SELECT ... FOR UPDATE} that also refreshes an entity this transaction already read: a plain locking
     * {@code find} on a managed entity takes the lock but keeps the stale state, and the later flush would write the
     * stale columns back over a parallel change (spec 0004 review 1, M-1).
     */
    private static <T> Optional<T> lockFresh(EntityManager em, Class<T> type, UUID id) {
        T entity = em.find(type, id);
        if (entity == null) {
            return Optional.empty();
        }
        em.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
        return Optional.of(entity);
    }
}
