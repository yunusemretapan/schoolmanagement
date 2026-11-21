package com.schoolos.application.features.institution.commands;

import static com.schoolos.application.features.institution.InstitutionFixture.TENANT_A;
import static com.schoolos.application.features.institution.Refusals.code;
import static org.assertj.core.api.Assertions.assertThat;

import com.schoolos.application.features.institution.InstitutionFixture;
import com.schoolos.application.features.institution.commands.TermCommands.ActivateTerm;
import com.schoolos.application.features.institution.commands.TermCommands.CreateTerm;
import com.schoolos.application.features.institution.commands.TermCommands.UpdateTerm;
import com.schoolos.application.features.institution.constants.InstitutionErrorCodes;
import com.schoolos.application.features.institution.dtos.AcademicStructure;
import com.schoolos.domain.institution.Term;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Spec 0005, Req. 4, 8 / AC-4: terms never share a day; exactly one active term once there is one. */
class TermCommandHandlersTest {

    private final InstitutionFixture fx = new InstitutionFixture();

    @BeforeEach
    void signIn() {
        fx.actAs(TENANT_A);
    }

    @AfterEach
    void signOut() {
        InstitutionFixture.signOut();
    }

    private static LocalDate d(String iso) {
        return LocalDate.parse(iso);
    }

    private AcademicStructure create(String name, String start, String end) {
        return fx.createTerm.handle(new CreateTerm(name, d(start), d(end)));
    }

    @Test
    void theFirstTermBecomesActiveAndALaterOneDoesNot() {
        AcademicStructure first = create("2026–2027", "2026-09-14", "2027-06-26");
        AcademicStructure second = create("2027–2028", "2027-09-13", "2028-06-24");

        assertThat(first.activeTermId()).isEqualTo(first.terms().get(0).id());
        assertThat(second.activeTermId()).isEqualTo(first.activeTermId());
        assertThat(second.terms())
                .filteredOn(AcademicStructure.TermView::active)
                .hasSize(1);
    }

    @Test
    void sharingEvenOneDayIsRefusedTheNextDayIsAccepted() {
        create("2026–2027", "2026-09-14", "2027-06-26");

        assertThat(code(() -> create("Yaz", "2027-06-26", "2027-08-31"))).isEqualTo(InstitutionErrorCodes.TERM_OVERLAP);
        assertThat(code(() -> create("Kış", "2026-01-01", "2026-09-14"))).isEqualTo(InstitutionErrorCodes.TERM_OVERLAP);
        assertThat(code(() -> create("Kapsayan", "2026-01-01", "2027-12-31")))
                .isEqualTo(InstitutionErrorCodes.TERM_OVERLAP);
        assertThat(create("Yaz okulu", "2027-06-27", "2027-08-31").terms()).hasSize(2);
    }

    @Test
    void changingDatesIntoAnotherTermIsRefusedEvenWhenTheTermHasClassGroups() {
        Term current = fx.seedTerm(TENANT_A, "2026–2027", "2026-09-14", "2027-06-26");
        Term next = fx.seedTerm(TENANT_A, "2027–2028", "2027-09-13", "2028-06-24");
        var level = fx.seedLevel(TENANT_A, "7. sınıf", 1);
        fx.seedGroup(TENANT_A, fx.campusOf(TENANT_A), next.id(), level.id(), "7/A");

        assertThat(code(() ->
                        fx.updateTerm.handle(new UpdateTerm(next.id(), next.name(), d("2027-06-26"), d("2028-06-24")))))
                .isEqualTo(InstitutionErrorCodes.TERM_OVERLAP);
        // its own dates never overlap itself; a term with class groups may still move within free days
        fx.updateTerm.handle(new UpdateTerm(next.id(), next.name(), d("2027-09-01"), d("2028-06-30")));
        assertThat(fx.terms.get(next.id()).startsOn()).isEqualTo(d("2027-09-01"));
        assertThat(fx.terms.get(current.id()).endsOn()).isEqualTo(d("2027-06-26"));
    }

    @Test
    void theSameNameIsRefusedWhateverTheCaseOrSpaces() {
        create("2026–2027 Güz", "2026-09-14", "2027-01-20");

        assertThat(code(() -> create("  2026–2027 GÜZ ", "2027-02-01", "2027-06-26")))
                .isEqualTo(InstitutionErrorCodes.NAME_TAKEN);
    }

    @Test
    void choosingAnotherActiveTermLeavesExactlyOneActive() {
        Term past = fx.seedTerm(TENANT_A, "2025–2026", "2025-09-08", "2026-06-19");
        Term current = fx.seedTerm(TENANT_A, "2026–2027", "2026-09-14", "2027-06-26");
        fx.activate(TENANT_A, current.id());

        AcademicStructure after = fx.activateTerm.handle(new ActivateTerm(past.id()));

        assertThat(after.activeTermId()).isEqualTo(past.id());
        assertThat(after.terms())
                .filteredOn(AcademicStructure.TermView::active)
                .extracting(AcademicStructure.TermView::id)
                .containsExactly(past.id());
    }

    @Test
    void theRulesRunUnderTheInstitutionLock() {
        create("2026–2027", "2026-09-14", "2027-06-26");

        // plan 0005, R3: two administrators at once are serialized by the settings row lock
        assertThat(fx.ops).containsSubsequence("lock", "check:term-name", "check:term-overlap");
        assertThat(fx.ops.get(0)).isEqualTo("lock");
    }
}
