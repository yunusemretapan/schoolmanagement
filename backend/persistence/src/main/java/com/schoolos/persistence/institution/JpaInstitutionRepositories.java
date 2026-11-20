package com.schoolos.persistence.institution;

import com.schoolos.application.features.institution.constants.InstitutionErrorCodes;
import com.schoolos.application.features.institution.ports.AcademicSettingsRepository;
import com.schoolos.application.features.institution.ports.CampusRepository;
import com.schoolos.application.features.institution.ports.ClassGroupRepository;
import com.schoolos.application.features.institution.ports.GradeLevelRepository;
import com.schoolos.application.features.institution.ports.TermRepository;
import com.schoolos.core.crosscutting.exceptions.BusinessException;
import com.schoolos.core.crosscutting.text.NameFold;
import com.schoolos.domain.institution.AcademicSettings;
import com.schoolos.domain.institution.Campus;
import com.schoolos.domain.institution.ClassGroup;
import com.schoolos.domain.institution.GradeLevel;
import com.schoolos.domain.institution.Term;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Repository;

/**
 * The academic structure of the bound institution (spec 0005). JPQL only, so the Hibernate tenant filter applies; RLS
 * holds the same line in the database (BR-1). No bulk DML (filters do not apply to it): rows are loaded, then removed.
 */
final class JpaInstitutionRepositories {

    private JpaInstitutionRepositories() {}

    /** The unique name indexes of V4__academic_structure.sql. */
    private static final Set<String> NAME_INDEXES =
            Set.of("campuses_name_uq", "terms_name_uq", "grade_levels_name_uq", "class_groups_name_uq");

    /**
     * Writes now, so that a unique index decides a race the business check lost: two administrators adding the same
     * name at once end in ACADEMIC_NAME_TAKEN (422), not in a 500 at commit (spec 0005, Req. 8; spec 0004 L-4).
     */
    static void flushNames(EntityManager em) {
        try {
            em.flush();
        } catch (PersistenceException e) {
            for (Throwable t = e; t != null; t = t.getCause()) {
                if (t instanceof ConstraintViolationException violation
                        && NAME_INDEXES.contains(violation.getConstraintName())) {
                    throw new BusinessException(InstitutionErrorCodes.NAME_TAKEN, "Name already in use");
                }
            }
            throw e;
        }
    }

    private static String folded(String path) {
        return NameFold.jpql(path) + " = :folded";
    }

    private static <T> Optional<T> one(TypedQuery<T> query) {
        return query.getResultStream().findFirst();
    }

    @Repository
    static class Settings implements AcademicSettingsRepository {

        @PersistenceContext
        private EntityManager em;

        @Override
        public Optional<AcademicSettings> lockForBoundTenant() {
            return one(em.createQuery("select s from AcademicSettingsEntity s", AcademicSettingsEntity.class)
                            .setLockMode(LockModeType.PESSIMISTIC_WRITE))
                    .map(AcademicSettingsEntity::toDomain);
        }

        @Override
        public Optional<AcademicSettings> findForBoundTenant() {
            return one(em.createQuery("select s from AcademicSettingsEntity s", AcademicSettingsEntity.class))
                    .map(AcademicSettingsEntity::toDomain);
        }

        @Override
        public void create(AcademicSettings settings) {
            em.persist(new AcademicSettingsEntity(settings));
        }

        @Override
        public void update(AcademicSettings settings) {
            em.createQuery("select s from AcademicSettingsEntity s where s.id = :id", AcademicSettingsEntity.class)
                    .setParameter("id", settings.id())
                    .getSingleResult()
                    .apply(settings);
        }
    }

    @Repository
    static class Campuses implements CampusRepository {

        @PersistenceContext
        private EntityManager em;

        @Override
        public List<Campus> list() {
            return em.createQuery("select c from CampusEntity c order by c.createdAt, c.id", CampusEntity.class)
                    .getResultStream()
                    .map(CampusEntity::toDomain)
                    .toList();
        }

        @Override
        public Optional<Campus> findById(UUID id) {
            return entity(id).map(CampusEntity::toDomain);
        }

        @Override
        public boolean nameTaken(String name, UUID exceptId) {
            String jpql = "select count(c) from CampusEntity c where " + folded("c.name")
                    + (exceptId == null ? "" : " and c.id <> :except");
            TypedQuery<Long> query = em.createQuery(jpql, Long.class).setParameter("folded", NameFold.fold(name));
            if (exceptId != null) {
                query.setParameter("except", exceptId);
            }
            return query.getSingleResult() > 0;
        }

        @Override
        public long count() {
            return em.createQuery("select count(c) from CampusEntity c", Long.class)
                    .getSingleResult();
        }

        @Override
        public void create(Campus campus) {
            em.persist(new CampusEntity(campus));
            flushNames(em);
        }

        @Override
        public void update(Campus campus) {
            entity(campus.id()).orElseThrow().apply(campus);
            flushNames(em);
        }

        @Override
        public void delete(UUID id) {
            entity(id).ifPresent(em::remove);
        }

        private Optional<CampusEntity> entity(UUID id) {
            return one(em.createQuery("select c from CampusEntity c where c.id = :id", CampusEntity.class)
                    .setParameter("id", id));
        }
    }

    @Repository
    static class Terms implements TermRepository {

        @PersistenceContext
        private EntityManager em;

        @Override
        public List<Term> list() {
            return em.createQuery("select t from TermEntity t order by t.startsOn desc, t.id", TermEntity.class)
                    .getResultStream()
                    .map(TermEntity::toDomain)
                    .toList();
        }

        @Override
        public Optional<Term> findById(UUID id) {
            return entity(id).map(TermEntity::toDomain);
        }

        @Override
        public boolean nameTaken(String name, UUID exceptId) {
            String jpql = "select count(t) from TermEntity t where " + folded("t.name")
                    + (exceptId == null ? "" : " and t.id <> :except");
            TypedQuery<Long> query = em.createQuery(jpql, Long.class).setParameter("folded", NameFold.fold(name));
            if (exceptId != null) {
                query.setParameter("except", exceptId);
            }
            return query.getSingleResult() > 0;
        }

        @Override
        public boolean overlapping(LocalDate startsOn, LocalDate endsOn, UUID exceptId) {
            String jpql = "select count(t) from TermEntity t where t.startsOn <= :end and t.endsOn >= :start"
                    + (exceptId == null ? "" : " and t.id <> :except");
            TypedQuery<Long> query = em.createQuery(jpql, Long.class)
                    .setParameter("start", startsOn)
                    .setParameter("end", endsOn);
            if (exceptId != null) {
                query.setParameter("except", exceptId);
            }
            return query.getSingleResult() > 0;
        }

        @Override
        public void create(Term term) {
            em.persist(new TermEntity(term));
            flushNames(em);
        }

        @Override
        public void update(Term term) {
            entity(term.id()).orElseThrow().apply(term);
            flushNames(em);
        }

        @Override
        public void delete(UUID id) {
            entity(id).ifPresent(em::remove);
        }

        private Optional<TermEntity> entity(UUID id) {
            return one(em.createQuery("select t from TermEntity t where t.id = :id", TermEntity.class)
                    .setParameter("id", id));
        }
    }

    @Repository
    static class GradeLevels implements GradeLevelRepository {

        @PersistenceContext
        private EntityManager em;

        @Override
        public List<GradeLevel> list() {
            return em.createQuery("select g from GradeLevelEntity g order by g.position", GradeLevelEntity.class)
                    .getResultStream()
                    .map(GradeLevelEntity::toDomain)
                    .toList();
        }

        @Override
        public Optional<GradeLevel> findById(UUID id) {
            return entity(id).map(GradeLevelEntity::toDomain);
        }

        @Override
        public boolean nameTaken(String name, UUID exceptId) {
            String jpql = "select count(g) from GradeLevelEntity g where " + folded("g.name")
                    + (exceptId == null ? "" : " and g.id <> :except");
            TypedQuery<Long> query = em.createQuery(jpql, Long.class).setParameter("folded", NameFold.fold(name));
            if (exceptId != null) {
                query.setParameter("except", exceptId);
            }
            return query.getSingleResult() > 0;
        }

        @Override
        public void create(GradeLevel level) {
            em.persist(new GradeLevelEntity(level));
            flushNames(em);
        }

        @Override
        public void update(List<GradeLevel> levels) {
            for (GradeLevel level : levels) {
                entity(level.id()).orElseThrow().apply(level);
            }
            flushNames(em);
        }

        @Override
        public void delete(UUID id) {
            entity(id).ifPresent(em::remove);
        }

        private Optional<GradeLevelEntity> entity(UUID id) {
            return one(em.createQuery("select g from GradeLevelEntity g where g.id = :id", GradeLevelEntity.class)
                    .setParameter("id", id));
        }
    }

    @Repository
    static class ClassGroups implements ClassGroupRepository {

        @PersistenceContext
        private EntityManager em;

        @Override
        public List<ClassGroup> list(UUID termId, Optional<UUID> campusId) {
            String jpql = "select c from ClassGroupEntity c where c.termId = :term"
                    + (campusId.isPresent() ? " and c.campusId = :campus" : "");
            TypedQuery<ClassGroupEntity> query =
                    em.createQuery(jpql, ClassGroupEntity.class).setParameter("term", termId);
            campusId.ifPresent(id -> query.setParameter("campus", id));
            return query.getResultStream().map(ClassGroupEntity::toDomain).toList();
        }

        @Override
        public Optional<ClassGroup> findById(UUID id) {
            return entity(id).map(ClassGroupEntity::toDomain);
        }

        @Override
        public boolean nameTaken(UUID campusId, UUID termId, String name, UUID exceptId) {
            String jpql = "select count(c) from ClassGroupEntity c where c.campusId = :campus and c.termId = :term and "
                    + folded("c.name") + (exceptId == null ? "" : " and c.id <> :except");
            TypedQuery<Long> query = em.createQuery(jpql, Long.class)
                    .setParameter("campus", campusId)
                    .setParameter("term", termId)
                    .setParameter("folded", NameFold.fold(name));
            if (exceptId != null) {
                query.setParameter("except", exceptId);
            }
            return query.getSingleResult() > 0;
        }

        @Override
        public boolean anyInCampus(UUID campusId) {
            return any("campusId", campusId);
        }

        @Override
        public boolean anyInTerm(UUID termId) {
            return any("termId", termId);
        }

        @Override
        public boolean anyOfGradeLevel(UUID gradeLevelId) {
            return any("gradeLevelId", gradeLevelId);
        }

        private boolean any(String field, UUID id) {
            return em.createQuery("select count(c) from ClassGroupEntity c where c." + field + " = :id", Long.class)
                            .setParameter("id", id)
                            .getSingleResult()
                    > 0;
        }

        @Override
        public void create(ClassGroup group) {
            em.persist(new ClassGroupEntity(group));
            flushNames(em);
        }

        @Override
        public void update(ClassGroup group) {
            entity(group.id()).orElseThrow().apply(group);
            flushNames(em);
        }

        @Override
        public void delete(UUID id) {
            entity(id).ifPresent(em::remove);
        }

        private Optional<ClassGroupEntity> entity(UUID id) {
            return one(em.createQuery("select c from ClassGroupEntity c where c.id = :id", ClassGroupEntity.class)
                    .setParameter("id", id));
        }
    }
}
