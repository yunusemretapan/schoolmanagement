package com.schoolos.application.features.institution.commands;

import static com.schoolos.application.features.institution.InstitutionFixture.TENANT_A;
import static com.schoolos.application.features.institution.Refusals.code;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.schoolos.application.features.institution.InstitutionFixture;
import com.schoolos.application.features.institution.commands.CampusCommands.CreateCampus;
import com.schoolos.application.features.institution.commands.ClassGroupCommands.CreateClassGroup;
import com.schoolos.application.features.institution.commands.ClassGroupCommands.UpdateClassGroup;
import com.schoolos.application.features.institution.constants.InstitutionErrorCodes;
import com.schoolos.application.features.institution.dtos.ClassGroupList;
import com.schoolos.application.features.institution.dtos.ClassGroupView;
import com.schoolos.application.features.institution.queries.AcademicQueries.ListClassGroups;
import com.schoolos.core.crosscutting.exceptions.ValidationException;
import com.schoolos.domain.institution.GradeLevel;
import com.schoolos.domain.institution.Term;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Spec 0005, Req. 6 / AC-6: class groups of a campus and a term, unique by name there, listed in level order. */
class ClassGroupCommandHandlersTest {

    private final InstitutionFixture fx = new InstitutionFixture();
    private Term current;
    private Term next;
    private GradeLevel seventh;
    private GradeLevel eighth;
    private UUID merkez;

    @BeforeEach
    void setUp() {
        fx.actAs(TENANT_A);
        current = fx.seedTerm(TENANT_A, "2026–2027", "2026-09-14", "2027-06-26");
        next = fx.seedTerm(TENANT_A, "2027–2028", "2027-09-13", "2028-06-24");
        fx.activate(TENANT_A, current.id());
        seventh = fx.seedLevel(TENANT_A, "7. sınıf", 1);
        eighth = fx.seedLevel(TENANT_A, "8. sınıf", 2);
        merkez = fx.campusOf(TENANT_A);
    }

    @AfterEach
    void signOut() {
        InstitutionFixture.signOut();
    }

    private ClassGroupView add(String name, GradeLevel level, UUID campus, UUID term) {
        return fx.createGroup.handle(new CreateClassGroup(name, level.id(), campus, term));
    }

    @Test
    void withOneCampusAndNoTermGivenItGoesToTheOnlyCampusAndTheActiveTerm() {
        ClassGroupView group = add(" 7/A ", seventh, null, null);

        assertThat(group.name()).isEqualTo("7/A");
        assertThat(group.campusId()).isEqualTo(merkez);
        assertThat(group.termId()).isEqualTo(current.id());
    }

    @Test
    void aTermThatIsNotActiveTakesClassGroupsToo() {
        assertThat(add("7/A", seventh, null, next.id()).termId()).isEqualTo(next.id());
    }

    @Test
    void withSeveralCampusesTheCampusMustBeChosen() {
        fx.createCampus.handle(new CreateCampus("Kadıköy"));

        assertThatThrownBy(() -> add("7/A", seventh, null, null))
                .isInstanceOfSatisfying(
                        ValidationException.class,
                        e -> assertThat(e.fieldErrors()).containsEntry("campusId", "NOT_NULL"));
    }

    @Test
    void theNameIsUniqueWithinCampusAndTermOnly() {
        add("7/A", seventh, merkez, current.id());
        UUID kadikoy = fx.createCampus
                .handle(new CreateCampus("Kadıköy"))
                .campuses()
                .get(1)
                .id();

        assertThat(code(() -> add("7/a", seventh, merkez, current.id()))).isEqualTo(InstitutionErrorCodes.NAME_TAKEN);
        assertThat(add("7/A", seventh, merkez, next.id()).termId()).isEqualTo(next.id());
        assertThat(add("7/A", seventh, kadikoy, current.id()).campusId()).isEqualTo(kadikoy);
    }

    @Test
    void nameAndLevelChangeCampusAndTermStay() {
        ClassGroupView group = add("7/A", seventh, merkez, current.id());

        ClassGroupView changed = fx.updateGroup.handle(new UpdateClassGroup(group.id(), "8/A", eighth.id()));

        assertThat(changed.name()).isEqualTo("8/A");
        assertThat(changed.gradeLevelId()).isEqualTo(eighth.id());
        assertThat(changed.campusId()).isEqualTo(merkez);
        assertThat(changed.termId()).isEqualTo(current.id());
    }

    @Test
    void theListShowsTheChosenTermInLevelOrderThenByName() {
        add("8/B", eighth, merkez, current.id());
        add("7/B", seventh, merkez, current.id());
        add("8/A", eighth, merkez, current.id());
        add("7/a", seventh, merkez, current.id());
        add("7/A", seventh, merkez, next.id());

        ClassGroupList active = fx.listGroups.handle(new ListClassGroups(null, null));
        ClassGroupList other = fx.listGroups.handle(new ListClassGroups(next.id(), null));

        assertThat(active.termId()).isEqualTo(current.id());
        assertThat(active.items()).extracting(ClassGroupView::name).containsExactly("7/a", "7/B", "8/A", "8/B");
        assertThat(other.items()).extracting(ClassGroupView::name).containsExactly("7/A");
    }

    @Test
    void withSeveralCampusesTheListFiltersByCampus() {
        UUID kadikoy = fx.createCampus
                .handle(new CreateCampus("Kadıköy"))
                .campuses()
                .get(1)
                .id();
        add("7/A", seventh, merkez, current.id());
        add("7/B", seventh, kadikoy, current.id());

        assertThat(fx.listGroups.handle(new ListClassGroups(null, kadikoy)).items())
                .extracting(ClassGroupView::name)
                .containsExactly("7/B");
        assertThat(fx.listGroups.handle(new ListClassGroups(null, null)).items())
                .hasSize(2);
    }

    @Test
    void withoutAnyTermThereIsNoClassGroupYet() {
        InstitutionFixture empty = new InstitutionFixture();
        empty.actAs(TENANT_A);
        GradeLevel level = empty.seedLevel(TENANT_A, "7. sınıf", 1);

        assertThat(code(() -> empty.createGroup.handle(new CreateClassGroup("7/A", level.id(), null, null))))
                .isEqualTo(InstitutionErrorCodes.NO_TERM);
        assertThat(empty.listGroups.handle(new ListClassGroups(null, null)))
                .isEqualTo(new ClassGroupList(null, java.util.List.of()));
    }
}
