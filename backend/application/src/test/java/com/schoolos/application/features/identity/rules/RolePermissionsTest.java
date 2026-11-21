package com.schoolos.application.features.identity.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.schoolos.core.security.Permission;
import java.util.List;
import org.junit.jupiter.api.Test;

class RolePermissionsTest {

    @Test
    void everyFaz1RoleCanReadItselfAndOnlyAdministratorsManageStaff() {
        for (String role : List.of("TEACHER", "GUARDIAN", "ACCOUNTANT")) {
            assertThat(RolePermissions.forRoleNames(List.of(role))).containsExactly(Permission.SELF_READ);
        }
        assertThat(RolePermissions.forRoleNames(List.of("ADMIN")))
                .containsExactlyInAnyOrder(
                        Permission.SELF_READ,
                        Permission.STAFF_MANAGE, // spec 0004, AC-1
                        Permission.ACADEMIC_STRUCTURE_MANAGE); // spec 0005, AC-1
    }

    @Test
    void noRoleIsAnOperatorAndUnknownRolesGrantNothing() {
        assertThat(RolePermissions.forRoleNames(List.of("ADMIN", "TEACHER", "GUARDIAN", "ACCOUNTANT")))
                .doesNotContain(Permission.PLATFORM_OPERATOR);
        assertThat(RolePermissions.forRoleNames(List.of("PLATFORM_OPERATOR", "admin")))
                .isEmpty();
    }
}
