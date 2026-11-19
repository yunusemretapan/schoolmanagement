package com.schoolos.application.features.identity.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.schoolos.core.security.Permission;
import java.util.List;
import org.junit.jupiter.api.Test;

class RolePermissionsTest {

    @Test
    void everyFaz1RoleCanReadItselfAndEndItsSession() {
        for (String role : List.of("ADMIN", "TEACHER", "GUARDIAN", "ACCOUNTANT")) {
            assertThat(RolePermissions.forRoleNames(List.of(role)))
                    .containsExactlyInAnyOrder(Permission.SELF_READ, Permission.SESSION_END);
        }
    }

    @Test
    void noRoleIsAnOperatorAndUnknownRolesGrantNothing() {
        assertThat(RolePermissions.forRoleNames(List.of("ADMIN", "TEACHER", "GUARDIAN", "ACCOUNTANT")))
                .doesNotContain(Permission.PLATFORM_OPERATOR);
        assertThat(RolePermissions.forRoleNames(List.of("PLATFORM_OPERATOR", "admin")))
                .isEmpty();
    }
}
