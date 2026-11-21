package com.schoolos.application.features.institution.commands;

import static com.schoolos.application.features.institution.InstitutionFixture.ADMIN_A;
import static com.schoolos.application.features.institution.InstitutionFixture.TENANT_A;
import static com.schoolos.application.features.institution.InstitutionFixture.TENANT_B;
import static com.schoolos.application.features.institution.Refusals.code;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.schoolos.application.features.institution.InstitutionFixture;
import com.schoolos.application.features.institution.commands.CampusCommands.CreateCampus;
import com.schoolos.application.features.institution.commands.CampusCommands.DeleteCampus;
import com.schoolos.application.features.institution.commands.CampusCommands.RenameCampus;
import com.schoolos.application.features.institution.commands.ClassGroupCommands.CreateClassGroup;
import com.schoolos.application.features.institution.commands.ClassGroupCommands.DeleteClassGroup;
import com.schoolos.application.features.institution.commands.ClassGroupCommands.UpdateClassGroup;
import com.schoolos.application.features.institution.commands.GradeLevelCommands.CreateGradeLevel;
import com.schoolos.application.features.institution.commands.GradeLevelCommands.DeleteGradeLevel;
import com.schoolos.application.features.institution.commands.GradeLevelCommands.Direction;
import com.schoolos.application.features.institution.commands.GradeLevelCommands.MoveGradeLevel;
import com.schoolos.application.features.institution.commands.GradeLevelCommands.RenameGradeLevel;
import com.schoolos.application.features.institution.commands.TermCommands.ActivateTerm;
import com.schoolos.application.features.institution.commands.TermCommands.CreateTerm;
import com.schoolos.application.features.institution.commands.TermCommands.DeleteTerm;
import com.schoolos.application.features.institution.commands.TermCommands.UpdateTerm;
import com.schoolos.application.features.institution.constants.InstitutionErrorCodes;
import com.schoolos.application.features.institution.dtos.AcademicStructure;
import com.schoolos.application.features.institution.queries.AcademicQueries.GetAcademicStructure;
import com.schoolos.application.features.institution.queries.AcademicQueries.ListClassGroups;
import com.schoolos.core.application.audit.AuditEntry;
import com.schoolos.core.crosscutting.exceptions.NotFoundException;
import com.schoolos.domain.institution.ClassGroup;
import com.schoolos.domain.institution.GradeLevel;
import com.schoolos.domain.institution.Term;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * Spec 0005, AC-2 (institution boundary), AC-8 (delete rules) and AC-10 (audit record) across the whole structure.
 * A is ours; B is another institution with its own campus, term, level and class group.
 */
class InstitutionCommandHandlersTest {

    private final InstitutionFixture fx = new InstitutionFixture();
    private Term termB;
    private GradeLevel levelB;
    private ClassGroup groupB;
    private UUID campusB;

    @BeforeEach
    void setUp() {
        campusB = fx.campusOf(TENANT_B);
        termB = fx.seedTerm(TENANT_B, "B 2026", "2026-09-14", "2027-06-26");
        fx.activate(TENANT_B, termB.id());
        levelB = fx.seedLevel(TENANT_B, "B seviye", 1);
        groupB = fx.seedGroup(TENANT_B, campusB, termB.id(), levelB.id(), "B/1");
        fx.actAs(TENANT_A);
    }

    @AfterEach
    void signOut() {
        InstitutionFixture.signOut();
    }

    @Test
    void anotherInstitutionsStructureIsNotListed() {
        AcademicStructure mine = fx.structure.handle(new GetAcademicStructure());

        assertThat(mine.campuses())
                .extracting(AcademicStructure.CampusView::name)
                .containsExactly("Merkez");
        assertThat(mine.terms()).isEmpty();
        assertThat(mine.gradeLevels()).isEmpty();
        assertThat(mine.activeTermId()).isNull();
    }

    @Test
    void anotherInstitutionsRecordsAnswerExactlyLikeUnknownIdsAndNothingChanges() throws Throwable {
        UUID unknown = UUID.randomUUID();
        LocalDate start = LocalDate.parse("2030-01-01");
        LocalDate end = LocalDate.parse("2030-06-01");
        for (UUID campus : new UUID[] {campusB, unknown}) {
            assertNotFound(() -> fx.renameCampus.handle(new RenameCampus(campus, "X")));
            assertNotFound(() -> fx.deleteCampus.handle(new DeleteCampus(campus)));
        }
        for (UUID term : new UUID[] {termB.id(), unknown}) {
            assertNotFound(() -> fx.updateTerm.handle(new UpdateTerm(term, "X", start, end)));
            assertNotFound(() -> fx.activateTerm.handle(new ActivateTerm(term)));
            assertNotFound(() -> fx.deleteTerm.handle(new DeleteTerm(term)));
            assertNotFound(() -> fx.listGroups.handle(new ListClassGroups(term, null)));
        }
        for (UUID level : new UUID[] {levelB.id(), unknown}) {
            assertNotFound(() -> fx.renameLevel.handle(new RenameGradeLevel(level, "X")));
            assertNotFound(() -> fx.moveLevel.handle(new MoveGradeLevel(level, Direction.UP)));
            assertNotFound(() -> fx.deleteLevel.handle(new DeleteGradeLevel(level)));
        }
        for (UUID group : new UUID[] {groupB.id(), unknown}) {
            assertNotFound(() -> fx.updateGroup.handle(new UpdateClassGroup(group, "X", levelB.id())));
            assertNotFound(() -> fx.deleteGroup.handle(new DeleteClassGroup(group)));
        }

        assertThat(fx.campuses.get(campusB).name()).isEqualTo("B Merkez");
        assertThat(fx.terms.get(termB.id()).name()).isEqualTo("B 2026");
        assertThat(fx.levels.get(levelB.id()).name()).isEqualTo("B seviye");
        assertThat(fx.groups.get(groupB.id()).name()).isEqualTo("B/1");
        assertThat(fx.settings.get(TENANT_A).activeTermId()).isNull();
        assertThat(fx.audit).isEmpty();
    }

    @Test
    void aClassGroupCannotBeBuiltFromAnotherInstitutionsCampusTermOrLevel() throws Throwable {
        Term mine = fx.seedTerm(TENANT_A, "2026–2027", "2026-09-14", "2027-06-26");
        fx.activate(TENANT_A, mine.id());
        GradeLevel myLevel = fx.seedLevel(TENANT_A, "7. sınıf", 1);
        ClassGroup myGroup = fx.seedGroup(TENANT_A, fx.campusOf(TENANT_A), mine.id(), myLevel.id(), "7/A");

        assertNotFound(() -> fx.createGroup.handle(new CreateClassGroup("7/B", myLevel.id(), campusB, null)));
        assertNotFound(() -> fx.createGroup.handle(new CreateClassGroup("7/B", myLevel.id(), null, termB.id())));
        assertNotFound(() -> fx.createGroup.handle(new CreateClassGroup("7/B", levelB.id(), null, null)));
        assertNotFound(() -> fx.updateGroup.handle(new UpdateClassGroup(myGroup.id(), "7/A", levelB.id())));

        assertThat(fx.groups.values())
                .filteredOn(g -> g.tenantId().equals(TENANT_A))
                .containsExactly(myGroup);
    }

    private static void assertNotFound(Executable action) {
        assertThatThrownBy(action::execute).isExactlyInstanceOf(NotFoundException.class);
    }

    @Test
    void theActiveTermAndRecordsWithClassGroupsAreNotDeletedUnusedOnesAre() {
        AcademicStructure s = fx.createTerm.handle(
                new CreateTerm("2026–2027", LocalDate.parse("2026-09-14"), LocalDate.parse("2027-06-26")));
        UUID active = s.activeTermId();
        UUID spare = fx.createTerm
                .handle(new CreateTerm("2027–2028", LocalDate.parse("2027-09-13"), LocalDate.parse("2028-06-24")))
                .terms()
                .get(0)
                .id();
        UUID used = fx.createLevel
                .handle(new CreateGradeLevel("7. sınıf"))
                .gradeLevels()
                .get(0)
                .id();
        UUID unused = fx.createLevel
                .handle(new CreateGradeLevel("8. sınıf"))
                .gradeLevels()
                .get(1)
                .id();
        fx.createGroup.handle(new CreateClassGroup("7/A", used, null, spare));

        assertThat(code(() -> fx.deleteTerm.handle(new DeleteTerm(active))))
                .isEqualTo(InstitutionErrorCodes.ACTIVE_TERM);
        assertThat(code(() -> fx.deleteTerm.handle(new DeleteTerm(spare)))).isEqualTo(InstitutionErrorCodes.IN_USE);
        assertThat(code(() -> fx.deleteLevel.handle(new DeleteGradeLevel(used))))
                .isEqualTo(InstitutionErrorCodes.IN_USE);
        AcademicStructure after = fx.deleteLevel.handle(new DeleteGradeLevel(unused));
        assertThat(after.gradeLevels())
                .extracting(AcademicStructure.GradeLevelView::id)
                .containsExactly(used);
        assertThat(after.terms())
                .filteredOn(AcademicStructure.TermView::inUse)
                .extracting(t -> t.id())
                .containsExactly(spare);
        UUID group = fx.listGroups
                .handle(new ListClassGroups(spare, null))
                .items()
                .get(0)
                .id();
        fx.deleteGroup.handle(new DeleteClassGroup(group));
        assertThat(fx.deleteTerm.handle(new DeleteTerm(spare)).terms()).hasSize(1);
    }

    @Test
    void everyChangeLeavesOneRecordWithWhoWhatWhichRecordAndWhen() {
        UUID campus = fx.createCampus
                .handle(new CreateCampus("Kadıköy"))
                .campuses()
                .get(1)
                .id();
        fx.renameCampus.handle(new RenameCampus(campus, "Kadıköy Şubesi"));
        UUID term = fx.createTerm
                .handle(new CreateTerm("2026–2027", LocalDate.parse("2026-09-14"), LocalDate.parse("2027-06-26")))
                .activeTermId();
        fx.updateTerm.handle(
                new UpdateTerm(term, "2026–2027", LocalDate.parse("2026-09-07"), LocalDate.parse("2027-06-26")));
        UUID level = fx.createLevel
                .handle(new CreateGradeLevel("7. sınıf"))
                .gradeLevels()
                .get(0)
                .id();
        UUID group = fx.createGroup
                .handle(new CreateClassGroup("7/A", level, campus, null))
                .id();
        fx.updateGroup.handle(new UpdateClassGroup(group, "7/B", level));
        fx.deleteGroup.handle(new DeleteClassGroup(group));
        fx.deleteCampus.handle(new DeleteCampus(campus));

        assertThat(fx.audit)
                .extracting(AuditEntry::action)
                .containsExactly(
                        "CAMPUS_CREATED",
                        "CAMPUS_RENAMED",
                        "TERM_CREATED",
                        "TERM_ACTIVATED",
                        "TERM_CHANGED",
                        "GRADE_LEVEL_CREATED",
                        "CLASS_GROUP_CREATED",
                        "CLASS_GROUP_CHANGED",
                        "CLASS_GROUP_DELETED",
                        "CAMPUS_DELETED");
        assertThat(fx.audit).allSatisfy(e -> {
            assertThat(e.actorUserId()).isEqualTo(ADMIN_A);
            assertThat(e.tenantId()).isEqualTo(TENANT_A);
            assertThat(e.at()).isEqualTo(fx.clock.instant());
            assertThat(e.targetName()).isNotBlank();
        });
        assertThat(fx.audit.get(1)).satisfies(e -> {
            assertThat(e.targetType()).isEqualTo("CAMPUS");
            assertThat(e.targetId()).isEqualTo(campus);
            assertThat(e.targetName()).isEqualTo("Kadıköy Şubesi");
        });
        assertThat(fx.audit.get(4).detail()).isEqualTo("startsOn");
        assertThat(fx.audit.get(7).detail()).isEqualTo("name");
    }

    @Test
    void aRefusedOrEmptyChangeLeavesNoRecord() {
        fx.createCampus.handle(new CreateCampus("Kadıköy"));
        int before = fx.audit.size();

        code(() -> fx.createCampus.handle(new CreateCampus("kadıköy")));
        code(() -> fx.renameCampus.handle(new RenameCampus(fx.campusOf(TENANT_A), "KADIKÖY")));
        fx.createTerm.handle(new CreateTerm(
                "Y",
                LocalDate.parse("2026-09-14"),
                LocalDate.parse("2026-09-14").plusDays(10)));
        code(() -> fx.createTerm.handle(
                new CreateTerm("Y2", LocalDate.parse("2026-09-20"), LocalDate.parse("2026-10-01"))));
        fx.renameCampus.handle(new RenameCampus(fx.campusOf(TENANT_A), "Merkez")); // same name: no change

        // the first term above is accepted and audited; everything after it is refused or unchanged
        assertThat(fx.audit.subList(before, fx.audit.size()))
                .extracting(AuditEntry::action)
                .containsExactly("TERM_CREATED", "TERM_ACTIVATED");
        assertThat(List.of(fx.audit.get(0).action())).containsExactly("CAMPUS_CREATED");
    }
}
