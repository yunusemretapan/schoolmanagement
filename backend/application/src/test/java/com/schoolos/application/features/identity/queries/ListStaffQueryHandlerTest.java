package com.schoolos.application.features.identity.queries;

import static org.assertj.core.api.Assertions.assertThat;

import com.schoolos.application.features.identity.StaffFixture;
import com.schoolos.application.features.identity.commands.StaffAccounts;
import com.schoolos.application.features.identity.dtos.StaffDetail;
import com.schoolos.application.features.identity.dtos.StaffPage;
import com.schoolos.application.features.identity.dtos.StaffSummary;
import com.schoolos.domain.identity.AccountStatus;
import com.schoolos.domain.identity.Role;
import com.schoolos.domain.identity.UserAccount;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Spec 0004, Req. 2, 3 / AC-2, AC-3; the detail view's flags for Req. 9. */
class ListStaffQueryHandlerTest {

    private final StaffFixture s = new StaffFixture();

    @BeforeEach
    void signIn() {
        s.actAs(s.admin);
    }

    @AfterEach
    void signOut() {
        StaffFixture.signOut();
    }

    @Test
    void listsOnlyTheCallersInstitutionByNameWithRolesAndStatus() {
        StaffPage page = s.list.handle(new ListStaffQuery(null, 0));

        assertThat(page.total()).isEqualTo(2);
        assertThat(page.items()).extracting(StaffSummary::fullName).containsExactly("Kurucu Ada", "Öğretmen Ali");
        StaffSummary teacher = page.items().get(1);
        assertThat(teacher.email()).isEqualTo("ali@ornek.test");
        assertThat(teacher.phone()).isEqualTo("+905321234567");
        assertThat(teacher.roles()).containsExactly("TEACHER");
        assertThat(teacher.status()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void anotherInstitutionsAdministratorSeesOnlyTheirOwn() {
        s.actAs(s.otherAdmin);

        assertThat(s.list.handle(new ListStaffQuery(null, 0)).items())
                .extracting(StaffSummary::fullName)
                .containsExactly("Başka Kurucu");
        assertThat(s.list.handle(new ListStaffQuery("ali", 0)).items()).isEmpty();
    }

    @Test
    void searchesByNameEmailOrAnyFormOfThePhoneNumber() {
        assertThat(s.list.handle(new ListStaffQuery("ALİ", 0)).items()).isEmpty(); // Turkish İ is not I
        assertThat(s.list.handle(new ListStaffQuery("ali", 0)).items())
                .extracting(StaffSummary::fullName)
                .containsExactly("Öğretmen Ali");
        assertThat(s.list.handle(new ListStaffQuery("KURUCU@", 0)).items())
                .extracting(StaffSummary::fullName)
                .containsExactly("Kurucu Ada");
        assertThat(s.list.handle(new ListStaffQuery("0532 123", 0)).items())
                .extracting(StaffSummary::fullName)
                .containsExactly("Öğretmen Ali");
        assertThat(s.list.handle(new ListStaffQuery("yok-boyle-biri", 0)).items())
                .isEmpty();
    }

    @Test
    void fiftyOneStaffMakeAPageOfFiftyAndAPageOfOne() {
        for (int i = 0; i < 49; i++) {
            s.seedStaff(String.format("Personel %02d", i), "p" + i + "@ornek.test", Role.TEACHER);
        }

        StaffPage first = s.list.handle(new ListStaffQuery(null, 0));
        StaffPage second = s.list.handle(new ListStaffQuery(null, 1));

        assertThat(first.total()).isEqualTo(51);
        assertThat(first.items()).hasSize(50);
        assertThat(first.pageSize()).isEqualTo(50);
        assertThat(second.items()).hasSize(1);
        assertThat(second.page()).isEqualTo(1);
    }

    @Test
    void guardianOnlyAccountsAreNotStaffButATeacherWhoIsAlsoAVeliShowsTheBadge() {
        s.fx.seedUserIn(s.admin.tenantId(), "Sadece Veli", "veli@ornek.test", null, "x", Role.GUARDIAN);
        s.seedStaff("Ayşe Öğretmen", "ayse@ornek.test", Role.TEACHER, Role.GUARDIAN);

        StaffPage page = s.list.handle(new ListStaffQuery(null, 0));

        assertThat(page.items()).extracting(StaffSummary::fullName).doesNotContain("Sadece Veli");
        assertThat(page.items())
                .filteredOn(item -> item.fullName().equals("Ayşe Öğretmen"))
                .singleElement()
                .satisfies(item -> assertThat(item.roles()).containsExactly("TEACHER", "GUARDIAN"));
    }

    @Test
    void theDetailFlagsTheCallerAndTheLastActiveAdministrator() {
        StaffDetail self = s.get.handle(new GetStaffQuery(s.admin.id()));
        StaffDetail teacher = s.get.handle(new GetStaffQuery(s.teacher.id()));

        assertThat(self.self()).isTrue();
        assertThat(self.lastActiveAdmin()).isTrue();
        assertThat(teacher.self()).isFalse();
        assertThat(teacher.lastActiveAdmin()).isFalse();

        UserAccount second = s.seedStaff("İkinci", "iki@ornek.test", Role.ADMIN);
        assertThat(s.get.handle(new GetStaffQuery(s.admin.id())).lastActiveAdmin())
                .isFalse();
        assertThat(StaffAccounts.caller().userId()).isEqualTo(s.admin.id());
        assertThat(second.roles()).containsExactly(Role.ADMIN);
    }
}
