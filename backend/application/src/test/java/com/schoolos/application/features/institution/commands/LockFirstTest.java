package com.schoolos.application.features.institution.commands;

import static com.schoolos.application.features.institution.InstitutionFixture.TENANT_A;
import static org.assertj.core.api.Assertions.assertThat;

import com.schoolos.application.features.institution.InstitutionFixture;
import com.schoolos.application.features.institution.commands.CampusCommands.CreateCampus;
import com.schoolos.application.features.institution.commands.CampusCommands.DeleteCampus;
import com.schoolos.application.features.institution.commands.CampusCommands.RenameCampus;
import com.schoolos.application.features.institution.commands.ClassGroupCommands.CreateClassGroup;
import com.schoolos.application.features.institution.commands.ClassGroupCommands.DeleteClassGroup;
import com.schoolos.application.features.institution.commands.ClassGroupCommands.UpdateClassGroup;
import com.schoolos.application.features.institution.commands.GradeLevelCommands.AddStandardGradeLevels;
import com.schoolos.application.features.institution.commands.GradeLevelCommands.CreateGradeLevel;
import com.schoolos.application.features.institution.commands.GradeLevelCommands.DeleteGradeLevel;
import com.schoolos.application.features.institution.commands.GradeLevelCommands.Direction;
import com.schoolos.application.features.institution.commands.GradeLevelCommands.MoveGradeLevel;
import com.schoolos.application.features.institution.commands.GradeLevelCommands.RenameGradeLevel;
import com.schoolos.application.features.institution.commands.TermCommands.ActivateTerm;
import com.schoolos.application.features.institution.commands.TermCommands.CreateTerm;
import com.schoolos.application.features.institution.commands.TermCommands.DeleteTerm;
import com.schoolos.application.features.institution.commands.TermCommands.UpdateTerm;
import com.schoolos.domain.institution.Campus;
import com.schoolos.domain.institution.ClassGroup;
import com.schoolos.domain.institution.GradeLevel;
import com.schoolos.domain.institution.Term;
import java.time.Instant;
import java.time.LocalDate;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Spec 0005, Req. 8 / plan 0005, R3 (review 1, L-2): every write takes the institution's settings lock before it reads
 * any record or checks any rule, so two administrators at once are serialized and no rule is checked on a stale view.
 */
class LockFirstTest {

    /** What every write needs: a second campus, two terms (one active), two levels and a class group. */
    record Seeded(
            InstitutionFixture fx,
            Campus spare,
            Term active,
            Term other,
            GradeLevel first,
            GradeLevel second,
            ClassGroup group) {}

    private static Seeded seed() {
        InstitutionFixture fx = new InstitutionFixture();
        fx.actAs(TENANT_A);
        Campus spare = new Campus(fx.ids.next(), TENANT_A, "Kadıköy", Instant.EPOCH);
        fx.campuses.put(spare.id(), spare);
        Term active = fx.seedTerm(TENANT_A, "2026–2027", "2026-09-14", "2027-06-26");
        Term other = fx.seedTerm(TENANT_A, "2027–2028", "2027-09-13", "2028-06-24");
        fx.activate(TENANT_A, active.id());
        GradeLevel first = fx.seedLevel(TENANT_A, "7. sınıf", 1);
        GradeLevel second = fx.seedLevel(TENANT_A, "8. sınıf", 2);
        ClassGroup group = fx.seedGroup(TENANT_A, fx.campusOf(TENANT_A), active.id(), first.id(), "7/A");
        fx.ops.clear();
        return new Seeded(fx, spare, active, other, first, second, group);
    }

    private static LocalDate d(String iso) {
        return LocalDate.parse(iso);
    }

    private static Named<Consumer<Seeded>> write(String name, Consumer<Seeded> call) {
        return Named.of(name, call);
    }

    static Stream<Named<Consumer<Seeded>>> writes() {
        return Stream.of(
                write("create campus", s -> s.fx().createCampus.handle(new CreateCampus("Ataşehir"))),
                write(
                        "rename campus",
                        s -> s.fx().renameCampus
                                .handle(new RenameCampus(s.spare().id(), "Moda"))),
                write(
                        "delete campus",
                        s -> s.fx().deleteCampus
                                .handle(new DeleteCampus(s.spare().id()))),
                write(
                        "create term",
                        s -> s.fx().createTerm.handle(new CreateTerm("2028–2029", d("2028-09-11"), d("2029-06-22")))),
                write(
                        "update term",
                        s -> s.fx().updateTerm
                                .handle(new UpdateTerm(s.other().id(), "2027–2028", d("2027-09-06"), d("2028-06-24")))),
                write(
                        "activate term",
                        s -> s.fx().activateTerm
                                .handle(new ActivateTerm(s.other().id()))),
                write(
                        "delete term",
                        s -> s.fx().deleteTerm.handle(new DeleteTerm(s.other().id()))),
                write("create level", s -> s.fx().createLevel.handle(new CreateGradeLevel("9. sınıf"))),
                write(
                        "rename level",
                        s -> s.fx().renameLevel
                                .handle(new RenameGradeLevel(s.second().id(), "Sekizinci"))),
                write(
                        "move level",
                        s -> s.fx().moveLevel
                                .handle(new MoveGradeLevel(s.second().id(), Direction.UP))),
                write(
                        "delete level",
                        s -> s.fx().deleteLevel
                                .handle(new DeleteGradeLevel(s.second().id()))),
                write("add standard levels", s -> {
                    s.fx().groups.clear();
                    s.fx().levels.clear();
                    s.fx().addStandard.handle(new AddStandardGradeLevels());
                }),
                write(
                        "create class group",
                        s -> s.fx().createGroup
                                .handle(new CreateClassGroup(
                                        "7/B",
                                        s.first().id(),
                                        s.fx().campusOf(TENANT_A),
                                        s.active().id()))),
                write(
                        "update class group",
                        s -> s.fx().updateGroup
                                .handle(new UpdateClassGroup(
                                        s.group().id(), "7/C", s.second().id()))),
                write(
                        "delete class group",
                        s -> s.fx().deleteGroup
                                .handle(new DeleteClassGroup(s.group().id()))));
    }

    @AfterEach
    void signOut() {
        InstitutionFixture.signOut();
    }

    @ParameterizedTest
    @MethodSource("writes")
    void theLockComesBeforeAnyReadOrRule(Consumer<Seeded> write) {
        Seeded seeded = seed();

        write.accept(seeded);

        assertThat(seeded.fx().ops).isNotEmpty();
        assertThat(seeded.fx().ops.get(0)).isEqualTo("lock");
        assertThat(seeded.fx().ops.subList(1, seeded.fx().ops.size())).doesNotContain("lock");
    }
}
