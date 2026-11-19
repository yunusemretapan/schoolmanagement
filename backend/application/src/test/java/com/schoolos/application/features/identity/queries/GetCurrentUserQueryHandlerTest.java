package com.schoolos.application.features.identity.queries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.schoolos.application.features.identity.IdentityFixture;
import com.schoolos.application.features.identity.dtos.CurrentUserResponse;
import com.schoolos.core.crosscutting.exceptions.NotFoundException;
import com.schoolos.core.security.CurrentUser;
import com.schoolos.core.security.CurrentUserHolder;
import com.schoolos.core.security.Permission;
import com.schoolos.domain.identity.Role;
import com.schoolos.domain.identity.UserAccount;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Spec 0003, AC-2 (home screen data) and AC-7 (only the token's tenant is visible). */
class GetCurrentUserQueryHandlerTest {

    private final IdentityFixture fx = new IdentityFixture();
    private final GetCurrentUserQueryHandler handler =
            new GetCurrentUserQueryHandler(fx.userRepository(), fx.tenantRepository());

    @AfterEach
    void clearUser() {
        CurrentUserHolder.clear();
    }

    private void signIn(UserAccount user, UUID tokenTenant) {
        CurrentUserHolder.set(
                new CurrentUser(user.id(), tokenTenant, UUID.randomUUID(), Set.of(), Set.of(Permission.SELF_READ)));
        fx.tenantSession().bind(tokenTenant); // what TransactionBehavior does from the token
    }

    @Test
    void returnsNameInstitutionAndEveryRole() {
        UserAccount user = fx.seedUser(
                "Örnek Koleji",
                "Ayşe Yılmaz",
                "ayse@ornek.test",
                null,
                "Ornek-Sifre-2026",
                Role.TEACHER,
                Role.GUARDIAN);
        signIn(user, user.tenantId());

        CurrentUserResponse me = handler.handle(new GetCurrentUserQuery());

        assertThat(me.fullName()).isEqualTo("Ayşe Yılmaz");
        assertThat(me.institutionName()).isEqualTo("Örnek Koleji");
        assertThat(me.roles()).containsExactly("TEACHER", "GUARDIAN");
    }

    @Test
    void aUserOfAnotherTenantIsNotVisibleUnderThisToken() {
        UserAccount inA = fx.seedUser("A Koleji", "A Kişi", "a@a.test", null, "Ornek-Sifre-2026", Role.ADMIN);
        UserAccount inB = fx.seedUser("B Koleji", "B Kişi", "b@b.test", null, "Ornek-Sifre-2026", Role.ADMIN);
        // A token for tenant A whose subject claims B's user id: B's data must not come back.
        CurrentUserHolder.set(
                new CurrentUser(inB.id(), inA.tenantId(), UUID.randomUUID(), Set.of(), Set.of(Permission.SELF_READ)));
        fx.tenantSession().bind(inA.tenantId());

        assertThatThrownBy(() -> handler.handle(new GetCurrentUserQuery())).isInstanceOf(NotFoundException.class);
    }
}
