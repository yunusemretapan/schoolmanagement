package com.schoolos.application.features.institution.commands;

import static org.assertj.core.api.Assertions.assertThat;

import com.schoolos.application.features.institution.commands.CampusCommands.CreateCampus;
import com.schoolos.application.features.institution.commands.ClassGroupCommands.CreateClassGroup;
import com.schoolos.application.features.institution.commands.TermCommands.CreateTerm;
import com.schoolos.application.features.institution.commands.TermCommands.UpdateTerm;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Spec 0005, Req. 4, 7 / AC-4, AC-7: names after trimming, term dates. */
class InstitutionValidatorsTest {

    private static LocalDate d(String iso) {
        return LocalDate.parse(iso);
    }

    @Test
    void namesAreMeasuredAfterTrimming60For20ForAClassGroup() {
        var campus = InstitutionValidators.createCampus();
        assertThat(campus.validate(new CreateCampus("x".repeat(60)))).isEmpty();
        assertThat(campus.validate(new CreateCampus("  " + "x".repeat(60) + "  ")))
                .isEmpty();
        assertThat(campus.validate(new CreateCampus("x".repeat(61)))).containsEntry("name", "SIZE");
        assertThat(campus.validate(new CreateCampus("   "))).containsEntry("name", "NOT_BLANK");
        assertThat(campus.validate(new CreateCampus(null))).containsEntry("name", "NOT_BLANK");

        var group = InstitutionValidators.createClassGroup();
        UUID level = UUID.randomUUID();
        assertThat(group.validate(new CreateClassGroup("x".repeat(20), level, null, null)))
                .isEmpty();
        assertThat(group.validate(new CreateClassGroup("x".repeat(21), level, null, null)))
                .containsEntry("name", "SIZE");
    }

    @Test
    void termDatesEndAfterStartWithin2000And2100AtMost731Days() {
        var create = InstitutionValidators.createTerm();
        assertThat(create.validate(
                        new CreateTerm("Y", d("2026-09-01"), d("2026-09-01").plusDays(730))))
                .isEmpty();
        assertThat(create.validate(
                        new CreateTerm("Y", d("2026-09-01"), d("2026-09-01").plusDays(731))))
                .containsEntry("endsOn", "TOO_LONG");
        assertThat(create.validate(new CreateTerm("Y", d("2026-09-14"), d("2026-09-14"))))
                .containsEntry("endsOn", "END_NOT_AFTER_START");
        assertThat(create.validate(new CreateTerm("Y", d("2000-01-01"), d("2000-06-30"))))
                .isEmpty();
        assertThat(create.validate(new CreateTerm("Y", d("1999-12-31"), d("2000-06-30"))))
                .containsEntry("startsOn", "OUT_OF_RANGE");
        assertThat(create.validate(new CreateTerm("Y", d("2100-06-01"), d("2100-12-31"))))
                .isEmpty();
        assertThat(create.validate(new CreateTerm("Y", d("2100-06-01"), d("2101-01-01"))))
                .containsEntry("endsOn", "OUT_OF_RANGE");
        assertThat(create.validate(new CreateTerm(" ", null, null)))
                .containsEntry("name", "NOT_BLANK")
                .containsEntry("startsOn", "NOT_NULL")
                .containsEntry("endsOn", "NOT_NULL");

        var update = InstitutionValidators.updateTerm();
        assertThat(update.validate(new UpdateTerm(UUID.randomUUID(), "Y", d("2026-09-14"), d("2026-09-13"))))
                .containsEntry("endsOn", "END_NOT_AFTER_START");
    }
}
