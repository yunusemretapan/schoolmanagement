package com.schoolos.application.features.institution.commands;

import static com.schoolos.application.features.institution.InstitutionFixture.TENANT_A;
import static com.schoolos.application.features.institution.Refusals.code;
import static org.assertj.core.api.Assertions.assertThat;

import com.schoolos.application.features.institution.InstitutionFixture;
import com.schoolos.application.features.institution.commands.GradeLevelCommands.AddStandardGradeLevels;
import com.schoolos.application.features.institution.commands.GradeLevelCommands.CreateGradeLevel;
import com.schoolos.application.features.institution.commands.GradeLevelCommands.DeleteGradeLevel;
import com.schoolos.application.features.institution.commands.GradeLevelCommands.Direction;
import com.schoolos.application.features.institution.commands.GradeLevelCommands.MoveGradeLevel;
import com.schoolos.application.features.institution.commands.GradeLevelCommands.RenameGradeLevel;
import com.schoolos.application.features.institution.constants.InstitutionErrorCodes;
import com.schoolos.application.features.institution.dtos.AcademicStructure;
import com.schoolos.application.features.institution.dtos.AcademicStructure.GradeLevelView;
import com.schoolos.domain.institution.StandardGradeLevels;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Spec 0005, Req. 5 / AC-5: the institution's own ordered list, at most 50, or the standard 14 in one step. */
class GradeLevelCommandHandlersTest {

    private final InstitutionFixture fx = new InstitutionFixture();

    @BeforeEach
    void signIn() {
        fx.actAs(TENANT_A);
    }

    @AfterEach
    void signOut() {
        InstitutionFixture.signOut();
    }

    private List<String> names(AcademicStructure structure) {
        return structure.gradeLevels().stream().map(GradeLevelView::name).toList();
    }

    @Test
    void addedLevelsGoToTheEndAndTheSameNameIsRefused() {
        fx.createLevel.handle(new CreateGradeLevel("LGS hazırlık"));
        AcademicStructure after = fx.createLevel.handle(new CreateGradeLevel("YKS"));

        assertThat(after.gradeLevels()).extracting(GradeLevelView::position).containsExactly(1, 2);
        assertThat(names(after)).containsExactly("LGS hazırlık", "YKS");
        assertThat(code(() -> fx.createLevel.handle(new CreateGradeLevel("lgs HAZIRLIK"))))
                .isEqualTo(InstitutionErrorCodes.NAME_TAKEN);
        UUID yks = after.gradeLevels().get(1).id();
        assertThat(names(fx.renameLevel.handle(new RenameGradeLevel(yks, "YKS hazırlık"))))
                .containsExactly("LGS hazırlık", "YKS hazırlık");
    }

    @Test
    void movingSwapsNeighboursTheEndsCannotMoveFurther() {
        fx.addStandard.handle(new AddStandardGradeLevels());
        List<GradeLevelView> levels = fx.structure
                .handle(
                        new com.schoolos.application.features.institution.queries.AcademicQueries
                                .GetAcademicStructure())
                .gradeLevels();
        UUID first = levels.get(0).id();
        UUID last = levels.get(levels.size() - 1).id();

        assertThat(code(() -> fx.moveLevel.handle(new MoveGradeLevel(first, Direction.UP))))
                .isEqualTo(InstitutionErrorCodes.CANNOT_MOVE);
        assertThat(code(() -> fx.moveLevel.handle(new MoveGradeLevel(last, Direction.DOWN))))
                .isEqualTo(InstitutionErrorCodes.CANNOT_MOVE);
        assertThat(names(fx.moveLevel.handle(new MoveGradeLevel(last, Direction.UP))))
                .endsWith("11. sınıf", "Hazırlık", "12. sınıf");
        assertThat(names(fx.moveLevel.handle(new MoveGradeLevel(first, Direction.DOWN))))
                .startsWith("1. sınıf", "Anaokulu", "2. sınıf");
    }

    @Test
    void theFiftiethLevelIsAcceptedTheFiftyFirstIsNot() {
        for (int i = 1; i <= 49; i++) {
            fx.seedLevel(TENANT_A, "Seviye " + i, i);
        }

        assertThat(fx.createLevel.handle(new CreateGradeLevel("Seviye 50")).gradeLevels())
                .hasSize(50);
        assertThat(code(() -> fx.createLevel.handle(new CreateGradeLevel("Seviye 51"))))
                .isEqualTo(InstitutionErrorCodes.GRADE_LEVEL_LIMIT);
    }

    @Test
    void theStandardListAddsFourteenInOrderOnlyWhileTheListIsEmpty() {
        AcademicStructure after = fx.addStandard.handle(new AddStandardGradeLevels());

        assertThat(names(after)).containsExactlyElementsOf(StandardGradeLevels.NAMES);
        assertThat(after.gradeLevels())
                .extracting(GradeLevelView::position)
                .startsWith(1, 2)
                .endsWith(14);
        assertThat(code(() -> fx.addStandard.handle(new AddStandardGradeLevels())))
                .isEqualTo(InstitutionErrorCodes.GRADE_LEVELS_NOT_EMPTY);
    }

    @Test
    void deletingALevelClosesTheGapAUsedLevelStays() {
        fx.addStandard.handle(new AddStandardGradeLevels());
        List<GradeLevelView> levels = fx.structure
                .handle(
                        new com.schoolos.application.features.institution.queries.AcademicQueries
                                .GetAcademicStructure())
                .gradeLevels();
        var term = fx.seedTerm(TENANT_A, "2026–2027", "2026-09-14", "2027-06-26");
        fx.seedGroup(TENANT_A, fx.campusOf(TENANT_A), term.id(), levels.get(7).id(), "7/A");

        AcademicStructure after =
                fx.deleteLevel.handle(new DeleteGradeLevel(levels.get(0).id()));

        assertThat(after.gradeLevels()).hasSize(13);
        assertThat(after.gradeLevels())
                .extracting(GradeLevelView::position)
                .startsWith(1, 2)
                .endsWith(13);
        assertThat(code(() ->
                        fx.deleteLevel.handle(new DeleteGradeLevel(levels.get(7).id()))))
                .isEqualTo(InstitutionErrorCodes.IN_USE);
    }
}
