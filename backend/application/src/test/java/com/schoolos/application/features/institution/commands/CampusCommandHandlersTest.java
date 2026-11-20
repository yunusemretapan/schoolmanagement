package com.schoolos.application.features.institution.commands;

import static com.schoolos.application.features.institution.InstitutionFixture.TENANT_A;
import static com.schoolos.application.features.institution.Refusals.code;
import static org.assertj.core.api.Assertions.assertThat;

import com.schoolos.application.features.identity.events.TenantProvisioned;
import com.schoolos.application.features.institution.InstitutionFixture;
import com.schoolos.application.features.institution.commands.CampusCommands.CreateCampus;
import com.schoolos.application.features.institution.commands.CampusCommands.DeleteCampus;
import com.schoolos.application.features.institution.commands.CampusCommands.RenameCampus;
import com.schoolos.application.features.institution.constants.InstitutionErrorCodes;
import com.schoolos.application.features.institution.dtos.AcademicStructure;
import com.schoolos.domain.institution.Campus;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Spec 0005, Req. 3, 9 / AC-3: every institution has at least one campus, named after it at first. */
class CampusCommandHandlersTest {

    private final InstitutionFixture fx = new InstitutionFixture();

    @BeforeEach
    void signIn() {
        fx.actAs(TENANT_A);
    }

    @AfterEach
    void signOut() {
        InstitutionFixture.signOut();
    }

    @Test
    void aNewInstitutionStartsWithOneCampusNamedAfterItAndItsSettingsRow() {
        UUID tenant = UUID.randomUUID();
        fx.bind(tenant);

        fx.defaultCampus.handle(new TenantProvisioned(tenant, "Örnek Koleji"));

        assertThat(fx.campuses.values())
                .filteredOn(c -> c.tenantId().equals(tenant))
                .extracting(Campus::name)
                .containsExactly("Örnek Koleji");
        assertThat(fx.settings.get(tenant).activeTermId()).isNull();
    }

    @Test
    void aLongInstitutionNameGivesTheCampusItsFirst60Characters() {
        UUID tenant = UUID.randomUUID();
        fx.bind(tenant);
        String name = "A".repeat(59) + "BC"; // 61 characters

        fx.defaultCampus.handle(new TenantProvisioned(tenant, name));

        assertThat(fx.campuses.values())
                .filteredOn(c -> c.tenantId().equals(tenant))
                .extracting(Campus::name)
                .containsExactly("A".repeat(59) + "B");
    }

    @Test
    void addAndRenameACampusTheSameNameIsRefused() {
        AcademicStructure added = fx.createCampus.handle(new CreateCampus("  Kadıköy "));
        UUID kadikoy = added.campuses().get(1).id();

        assertThat(added.campuses())
                .extracting(AcademicStructure.CampusView::name)
                .containsExactly("Merkez", "Kadıköy");
        assertThat(code(() -> fx.createCampus.handle(new CreateCampus("KADIKÖY"))))
                .isEqualTo(InstitutionErrorCodes.NAME_TAKEN);
        assertThat(code(() -> fx.renameCampus.handle(new RenameCampus(kadikoy, "merkez"))))
                .isEqualTo(InstitutionErrorCodes.NAME_TAKEN);
        assertThat(fx.renameCampus
                        .handle(new RenameCampus(kadikoy, "Kadıköy Şubesi"))
                        .campuses())
                .extracting(AcademicStructure.CampusView::name)
                .containsExactly("Merkez", "Kadıköy Şubesi");
    }

    @Test
    void theLastCampusIsNeverDeletedAnUnusedSecondOneIs() {
        UUID merkez = fx.campusOf(TENANT_A);

        assertThat(code(() -> fx.deleteCampus.handle(new DeleteCampus(merkez))))
                .isEqualTo(InstitutionErrorCodes.LAST_CAMPUS);

        UUID second = fx.createCampus
                .handle(new CreateCampus("Kadıköy"))
                .campuses()
                .get(1)
                .id();
        assertThat(fx.deleteCampus.handle(new DeleteCampus(second)).campuses())
                .extracting(AcademicStructure.CampusView::id)
                .containsExactly(merkez);
    }

    @Test
    void aCampusWithClassGroupsIsNotDeleted() {
        UUID merkez = fx.campusOf(TENANT_A);
        fx.createCampus.handle(new CreateCampus("Kadıköy"));
        var term = fx.seedTerm(TENANT_A, "2026–2027", "2026-09-14", "2027-06-26");
        var level = fx.seedLevel(TENANT_A, "7. sınıf", 1);
        fx.seedGroup(TENANT_A, merkez, term.id(), level.id(), "7/A");

        assertThat(code(() -> fx.deleteCampus.handle(new DeleteCampus(merkez))))
                .isEqualTo(InstitutionErrorCodes.IN_USE);
        assertThat(fx.campuses).containsKey(merkez);
    }
}
