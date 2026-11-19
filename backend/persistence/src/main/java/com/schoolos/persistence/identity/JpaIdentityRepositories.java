package com.schoolos.persistence.identity;

import com.schoolos.application.features.identity.ports.LoginIdentityRepository;
import com.schoolos.application.features.identity.ports.SessionRepository;
import com.schoolos.application.features.identity.ports.TenantRepository;
import com.schoolos.application.features.identity.ports.UserRepository;
import com.schoolos.domain.identity.LoginIdentity;
import com.schoolos.domain.identity.Session;
import com.schoolos.domain.identity.Tenant;
import com.schoolos.domain.identity.UserAccount;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;
import java.util.UUID;
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
    static class Users implements UserRepository {

        @PersistenceContext
        private EntityManager em;

        @Override
        public Optional<UserAccount> findById(UUID userId) {
            return em.createQuery("select u from UserEntity u where u.id = :id", UserEntity.class)
                    .setParameter("id", userId)
                    .getResultStream()
                    .findFirst()
                    .map(UserEntity::toDomain);
        }

        @Override
        public void create(UserAccount user) {
            em.persist(new UserEntity(user));
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
        }

        @Override
        public void update(LoginIdentity identity) {
            LoginIdentityEntity entity = em.find(LoginIdentityEntity.class, identity.userId());
            entity.apply(identity);
        }
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
    }
}
