package com.schoolos.application.features.institution;

import com.schoolos.application.features.institution.commands.CampusHandlers;
import com.schoolos.application.features.institution.commands.ClassGroupHandlers;
import com.schoolos.application.features.institution.commands.GradeLevelHandlers;
import com.schoolos.application.features.institution.commands.TermHandlers;
import com.schoolos.application.features.institution.events.CreateDefaultCampusOnTenantProvisioned;
import com.schoolos.application.features.institution.ports.AcademicSettingsRepository;
import com.schoolos.application.features.institution.ports.CampusRepository;
import com.schoolos.application.features.institution.ports.ClassGroupRepository;
import com.schoolos.application.features.institution.ports.GradeLevelRepository;
import com.schoolos.application.features.institution.ports.TermRepository;
import com.schoolos.application.features.institution.queries.AcademicQueryHandlers;
import com.schoolos.application.features.institution.rules.AcademicStructureReader;
import com.schoolos.application.features.institution.rules.AcademicWrites;
import com.schoolos.core.application.audit.AuditEntry;
import com.schoolos.core.application.audit.AuditLog;
import com.schoolos.core.crosscutting.ids.IdGenerator;
import com.schoolos.core.crosscutting.text.NameFold;
import com.schoolos.core.security.CurrentUser;
import com.schoolos.core.security.CurrentUserHolder;
import com.schoolos.core.security.Permission;
import com.schoolos.domain.institution.AcademicSettings;
import com.schoolos.domain.institution.Campus;
import com.schoolos.domain.institution.ClassGroup;
import com.schoolos.domain.institution.GradeLevel;
import com.schoolos.domain.institution.Term;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * In-memory academic structure for two institutions, A (ours) and B. Every repository sees only the bound tenant, as
 * the Hibernate filter and RLS do (BR-1); {@link #ops} records the settings lock and each rule check in order, so a
 * test can prove the lock comes first (plan 0005, R3). Inserts into another tenant fail, as RLS WITH CHECK would.
 */
public final class InstitutionFixture {

    public static final UUID TENANT_A = UUID.fromString("01900000-0000-7000-8000-00000000000a");
    public static final UUID TENANT_B = UUID.fromString("01900000-0000-7000-8000-00000000000b");
    public static final UUID ADMIN_A = UUID.fromString("01900000-0000-7000-8000-0000000000a1");
    public static final UUID ADMIN_B = UUID.fromString("01900000-0000-7000-8000-0000000000b1");

    public final Map<UUID, AcademicSettings> settings = new LinkedHashMap<>();
    public final Map<UUID, Campus> campuses = new LinkedHashMap<>();
    public final Map<UUID, Term> terms = new LinkedHashMap<>();
    public final Map<UUID, GradeLevel> levels = new LinkedHashMap<>();
    public final Map<UUID, ClassGroup> groups = new LinkedHashMap<>();
    public final List<AuditEntry> audit = new ArrayList<>();
    /** "lock" for the settings row, "check:<rule>" for each rule read, in call order. */
    public final List<String> ops = new ArrayList<>();

    public final Clock clock = Clock.fixed(Instant.parse("2026-11-21T09:00:00Z"), ZoneOffset.UTC);
    private final AtomicInteger counter = new AtomicInteger(100);
    public final IdGenerator ids =
            () -> UUID.fromString("01900000-0000-7000-8000-%012d".formatted(counter.incrementAndGet()));

    private UUID bound;

    public final AcademicSettingsRepository settingsRepository = new Settings();
    public final CampusRepository campusRepository = new Campuses();
    public final TermRepository termRepository = new Terms();
    public final GradeLevelRepository levelRepository = new Levels();
    public final ClassGroupRepository groupRepository = new Groups();
    public final AuditLog auditLog = entry -> {
        if (!entry.tenantId().equals(bound)) {
            throw new IllegalStateException("RLS WITH CHECK would refuse this insert");
        }
        audit.add(entry);
    };

    public final AcademicWrites writes = new AcademicWrites(settingsRepository, auditLog, ids, clock);
    public final AcademicStructureReader reader = new AcademicStructureReader(
            settingsRepository, termRepository, campusRepository, levelRepository, groupRepository);

    public final CampusHandlers.Create createCampus = new CampusHandlers.Create(campusRepository, writes, reader);
    public final CampusHandlers.Rename renameCampus = new CampusHandlers.Rename(campusRepository, writes, reader);
    public final CampusHandlers.Delete deleteCampus =
            new CampusHandlers.Delete(campusRepository, groupRepository, writes, reader);
    public final TermHandlers.Create createTerm = new TermHandlers.Create(termRepository, writes, reader);
    public final TermHandlers.Update updateTerm = new TermHandlers.Update(termRepository, writes, reader);
    public final TermHandlers.Activate activateTerm = new TermHandlers.Activate(termRepository, writes, reader);
    public final TermHandlers.Delete deleteTerm =
            new TermHandlers.Delete(termRepository, groupRepository, writes, reader);
    public final GradeLevelHandlers.Create createLevel = new GradeLevelHandlers.Create(levelRepository, writes, reader);
    public final GradeLevelHandlers.Rename renameLevel = new GradeLevelHandlers.Rename(levelRepository, writes, reader);
    public final GradeLevelHandlers.Move moveLevel = new GradeLevelHandlers.Move(levelRepository, writes, reader);
    public final GradeLevelHandlers.Delete deleteLevel =
            new GradeLevelHandlers.Delete(levelRepository, groupRepository, writes, reader);
    public final GradeLevelHandlers.AddStandard addStandard =
            new GradeLevelHandlers.AddStandard(levelRepository, writes, reader);
    public final ClassGroupHandlers.Create createGroup =
            new ClassGroupHandlers.Create(groupRepository, campusRepository, termRepository, levelRepository, writes);
    public final ClassGroupHandlers.Update updateGroup =
            new ClassGroupHandlers.Update(groupRepository, levelRepository, writes);
    public final ClassGroupHandlers.Delete deleteGroup = new ClassGroupHandlers.Delete(groupRepository, writes);
    public final AcademicQueryHandlers.Structure structure = new AcademicQueryHandlers.Structure(reader);
    public final AcademicQueryHandlers.ClassGroups listGroups = new AcademicQueryHandlers.ClassGroups(
            settingsRepository, termRepository, campusRepository, levelRepository, groupRepository);
    public final CreateDefaultCampusOnTenantProvisioned defaultCampus =
            new CreateDefaultCampusOnTenantProvisioned(settingsRepository, campusRepository, ids, clock);

    /** Institutions A and B as a migrated institution has them: a settings row and one campus each. */
    public InstitutionFixture() {
        for (UUID tenant : List.of(TENANT_A, TENANT_B)) {
            settings.put(tenant, new AcademicSettings(ids.next(), tenant, null));
            Campus campus = new Campus(ids.next(), tenant, tenant == TENANT_A ? "Merkez" : "B Merkez", Instant.EPOCH);
            campuses.put(campus.id(), campus);
        }
    }

    /** Signs in as A's or B's administrator, with that tenant bound. */
    public void actAs(UUID tenant) {
        bound = tenant;
        CurrentUserHolder.set(new CurrentUser(
                tenant == TENANT_A ? ADMIN_A : ADMIN_B,
                tenant,
                UUID.randomUUID(),
                Set.of("ADMIN"),
                Set.of(Permission.ACADEMIC_STRUCTURE_MANAGE)));
    }

    /** Binds a tenant without a signed-in user (the provisioning transaction). */
    public void bind(UUID tenant) {
        bound = tenant;
    }

    public static void signOut() {
        CurrentUserHolder.clear();
    }

    public UUID campusOf(UUID tenant) {
        return campuses.values().stream()
                .filter(c -> c.tenantId().equals(tenant))
                .findFirst()
                .orElseThrow()
                .id();
    }

    /** Seeds directly (no handler, no audit), into the given tenant. */
    public Term seedTerm(UUID tenant, String name, String start, String end) {
        Term term = new Term(ids.next(), tenant, name, LocalDate.parse(start), LocalDate.parse(end), Instant.EPOCH);
        terms.put(term.id(), term);
        return term;
    }

    public GradeLevel seedLevel(UUID tenant, String name, int position) {
        GradeLevel level = new GradeLevel(ids.next(), tenant, name, position, Instant.EPOCH);
        levels.put(level.id(), level);
        return level;
    }

    public ClassGroup seedGroup(UUID tenant, UUID campus, UUID term, UUID level, String name) {
        ClassGroup group = new ClassGroup(ids.next(), tenant, campus, term, level, name, Instant.EPOCH);
        groups.put(group.id(), group);
        return group;
    }

    public void activate(UUID tenant, UUID termId) {
        settings.put(tenant, settings.get(tenant).withActiveTerm(termId));
    }

    private boolean mine(UUID tenant) {
        return tenant.equals(bound);
    }

    private void ensureMine(UUID tenant) {
        if (!mine(tenant)) {
            throw new IllegalStateException("RLS WITH CHECK would refuse this write");
        }
    }

    private static boolean sameName(String a, String b) {
        return NameFold.fold(a).equals(NameFold.fold(b));
    }

    private final class Settings implements AcademicSettingsRepository {
        @Override
        public Optional<AcademicSettings> lockForBoundTenant() {
            ops.add("lock");
            return findForBoundTenant();
        }

        @Override
        public Optional<AcademicSettings> findForBoundTenant() {
            return Optional.ofNullable(bound == null ? null : settings.get(bound));
        }

        @Override
        public void create(AcademicSettings value) {
            ensureMine(value.tenantId());
            settings.put(value.tenantId(), value);
        }

        @Override
        public void update(AcademicSettings value) {
            ensureMine(value.tenantId());
            settings.put(value.tenantId(), value);
        }
    }

    private final class Campuses implements CampusRepository {
        @Override
        public List<Campus> list() {
            return campuses.values().stream().filter(c -> mine(c.tenantId())).toList();
        }

        @Override
        public Optional<Campus> findById(UUID id) {
            return Optional.ofNullable(campuses.get(id)).filter(c -> mine(c.tenantId()));
        }

        @Override
        public boolean nameTaken(String name, UUID exceptId) {
            ops.add("check:campus-name");
            return list().stream().anyMatch(c -> !c.id().equals(exceptId) && sameName(c.name(), name));
        }

        @Override
        public long count() {
            ops.add("check:campus-count");
            return list().size();
        }

        @Override
        public void create(Campus campus) {
            ensureMine(campus.tenantId());
            campuses.put(campus.id(), campus);
        }

        @Override
        public void update(Campus campus) {
            ensureMine(campus.tenantId());
            campuses.put(campus.id(), campus);
        }

        @Override
        public void delete(UUID id) {
            findById(id).ifPresent(c -> campuses.remove(id));
        }
    }

    private final class Terms implements TermRepository {
        @Override
        public List<Term> list() {
            return terms.values().stream()
                    .filter(t -> mine(t.tenantId()))
                    .sorted(Comparator.comparing(Term::startsOn).reversed())
                    .toList();
        }

        @Override
        public Optional<Term> findById(UUID id) {
            return Optional.ofNullable(terms.get(id)).filter(t -> mine(t.tenantId()));
        }

        @Override
        public boolean nameTaken(String name, UUID exceptId) {
            ops.add("check:term-name");
            return list().stream().anyMatch(t -> !t.id().equals(exceptId) && sameName(t.name(), name));
        }

        @Override
        public boolean overlapping(LocalDate startsOn, LocalDate endsOn, UUID exceptId) {
            ops.add("check:term-overlap");
            return list().stream().anyMatch(t -> !t.id().equals(exceptId) && t.overlaps(startsOn, endsOn));
        }

        @Override
        public void create(Term term) {
            ensureMine(term.tenantId());
            terms.put(term.id(), term);
        }

        @Override
        public void update(Term term) {
            ensureMine(term.tenantId());
            terms.put(term.id(), term);
        }

        @Override
        public void delete(UUID id) {
            findById(id).ifPresent(t -> terms.remove(id));
        }
    }

    private final class Levels implements GradeLevelRepository {
        @Override
        public List<GradeLevel> list() {
            ops.add("check:levels");
            return levels.values().stream()
                    .filter(l -> mine(l.tenantId()))
                    .sorted(Comparator.comparingInt(GradeLevel::position))
                    .toList();
        }

        @Override
        public Optional<GradeLevel> findById(UUID id) {
            return Optional.ofNullable(levels.get(id)).filter(l -> mine(l.tenantId()));
        }

        @Override
        public boolean nameTaken(String name, UUID exceptId) {
            ops.add("check:level-name");
            return levels.values().stream()
                    .filter(l -> mine(l.tenantId()))
                    .anyMatch(l -> !l.id().equals(exceptId) && sameName(l.name(), name));
        }

        @Override
        public void create(GradeLevel level) {
            ensureMine(level.tenantId());
            levels.put(level.id(), level);
        }

        @Override
        public void update(List<GradeLevel> changed) {
            for (GradeLevel level : changed) {
                ensureMine(level.tenantId());
                levels.put(level.id(), level);
            }
            Map<Integer, Long> byPosition = levels.values().stream()
                    .filter(l -> mine(l.tenantId()))
                    .collect(Collectors.groupingBy(GradeLevel::position, Collectors.counting()));
            if (byPosition.values().stream().anyMatch(n -> n > 1)) {
                throw new IllegalStateException("grade_levels_position_uq would refuse this at commit");
            }
        }

        @Override
        public void delete(UUID id) {
            findById(id).ifPresent(l -> levels.remove(id));
        }
    }

    private final class Groups implements ClassGroupRepository {
        private List<ClassGroup> mineOnly() {
            return groups.values().stream().filter(g -> mine(g.tenantId())).toList();
        }

        @Override
        public List<ClassGroup> list(UUID termId, Optional<UUID> campusId) {
            return mineOnly().stream()
                    .filter(g -> g.termId().equals(termId))
                    .filter(g -> campusId.map(g.campusId()::equals).orElse(true))
                    .toList();
        }

        @Override
        public Optional<ClassGroup> findById(UUID id) {
            return Optional.ofNullable(groups.get(id)).filter(g -> mine(g.tenantId()));
        }

        @Override
        public boolean nameTaken(UUID campusId, UUID termId, String name, UUID exceptId) {
            ops.add("check:group-name");
            return mineOnly().stream()
                    .anyMatch(g -> g.campusId().equals(campusId)
                            && g.termId().equals(termId)
                            && !g.id().equals(exceptId)
                            && sameName(g.name(), name));
        }

        private boolean any(Predicate<ClassGroup> rule, String check) {
            ops.add("check:" + check);
            return mineOnly().stream().anyMatch(rule);
        }

        @Override
        public boolean anyInCampus(UUID campusId) {
            return any(g -> g.campusId().equals(campusId), "campus-in-use");
        }

        @Override
        public boolean anyInTerm(UUID termId) {
            return any(g -> g.termId().equals(termId), "term-in-use");
        }

        @Override
        public boolean anyOfGradeLevel(UUID gradeLevelId) {
            return any(g -> g.gradeLevelId().equals(gradeLevelId), "level-in-use");
        }

        @Override
        public Set<UUID> usedCampusIds() {
            return mineOnly().stream().map(ClassGroup::campusId).collect(Collectors.toSet());
        }

        @Override
        public Set<UUID> usedTermIds() {
            return mineOnly().stream().map(ClassGroup::termId).collect(Collectors.toSet());
        }

        @Override
        public Set<UUID> usedGradeLevelIds() {
            return mineOnly().stream().map(ClassGroup::gradeLevelId).collect(Collectors.toSet());
        }

        @Override
        public void create(ClassGroup group) {
            ensureMine(group.tenantId());
            if (!campuses.containsKey(group.campusId())
                    || !campuses.get(group.campusId()).tenantId().equals(group.tenantId())
                    || !terms.get(group.termId()).tenantId().equals(group.tenantId())
                    || !levels.get(group.gradeLevelId()).tenantId().equals(group.tenantId())) {
                throw new IllegalStateException("The composite tenant foreign keys would refuse this row");
            }
            groups.put(group.id(), group);
        }

        @Override
        public void update(ClassGroup group) {
            ensureMine(group.tenantId());
            groups.put(group.id(), group);
        }

        @Override
        public void delete(UUID id) {
            findById(id).ifPresent(g -> groups.remove(id));
        }
    }
}
