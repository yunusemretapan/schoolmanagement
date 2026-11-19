package com.schoolos.application.features.identity.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.schoolos.application.features.identity.IdentityFixture;
import com.schoolos.application.features.identity.constants.IdentityErrorCodes;
import com.schoolos.application.features.identity.dtos.ProvisionedAccount;
import com.schoolos.core.crosscutting.exceptions.BusinessException;
import com.schoolos.domain.identity.LoginIdentity;
import com.schoolos.domain.identity.Role;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Spec 0003, AC-1: the operator creates an institution and its founder; identifiers are unique platform-wide. */
class ProvisionTenantCommandHandlerTest {

    private final IdentityFixture fx = new IdentityFixture();

    private final ProvisionTenantCommandHandler provision = new ProvisionTenantCommandHandler(
            fx.tenantRepository(),
            fx.userRepository(),
            fx.identityRepository(),
            fx.tenantSession(),
            fx.passwords,
            fx.ids,
            fx.clock);

    private final CreateSeedUserCommandHandler seed = new CreateSeedUserCommandHandler(
            fx.userRepository(), fx.identityRepository(), fx.tenantSession(), fx.passwords, fx.ids, fx.clock);

    @Test
    void createsTheInstitutionAndItsFounderAsAdmin() {
        ProvisionedAccount created = provision.handle(new ProvisionTenantCommand(
                " Örnek Koleji ", "Kurucu Bey", "Kurucu@Ornek.test", "0532 000 00 01", "Ornek-Sifre-2026"));

        assertThat(fx.tenants.get(created.tenantId()).name()).isEqualTo("Örnek Koleji");
        assertThat(fx.users.get(created.userId()).roles()).containsExactly(Role.ADMIN);
        LoginIdentity identity = fx.identities.get(created.userId());
        assertThat(identity.email()).isEqualTo("kurucu@ornek.test");
        assertThat(identity.phone()).isEqualTo("+905320000001");
        assertThat(identity.passwordHash()).isEqualTo("hashed:Ornek-Sifre-2026");
        assertThat(identity.tenantId()).isEqualTo(created.tenantId());
        assertThat(fx.bindings).containsExactly(created.tenantId());
    }

    @Test
    void anEmailInUseIsRefusedEvenForAnotherInstitution() {
        provision.handle(new ProvisionTenantCommand("A Koleji", "A", "ortak@ornek.test", null, "Ornek-Sifre-2026"));

        assertThatThrownBy(() -> provision.handle(
                        new ProvisionTenantCommand("B Koleji", "B", "ORTAK@ornek.test", null, "Ornek-Sifre-2026")))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(IdentityErrorCodes.IDENTIFIER_TAKEN));
        assertThat(fx.tenants).hasSize(1);
    }

    @Test
    void aPhoneInUseIsRefusedWhateverItsSpelling() {
        ProvisionedAccount a =
                provision.handle(new ProvisionTenantCommand("A Koleji", "A", null, "05320000001", "Ornek-Sifre-2026"));

        assertThatThrownBy(() -> seed.handle(new CreateSeedUserCommand(
                        a.tenantId(), "B", null, "+90 532 000 00 01", "Ornek-Sifre-2026", Set.of(Role.TEACHER))))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void passwordsShorterThanTenCharactersAndMissingIdentifiersAreRejected() {
        ProvisionTenantCommandValidator validator = new ProvisionTenantCommandValidator();

        assertThat(validator.validate(new ProvisionTenantCommand("A", "A", "a@a.test", null, "123456789")))
                .containsEntry("password", IdentityErrorCodes.PASSWORD_TOO_SHORT);
        assertThat(validator.validate(new ProvisionTenantCommand("A", "A", "a@a.test", null, "1234567890")))
                .isEmpty();
        assertThat(validator.validate(new ProvisionTenantCommand("A", "A", null, " ", "1234567890")))
                .containsEntry("email", IdentityErrorCodes.IDENTIFIER_REQUIRED);
        assertThat(validator.validate(new ProvisionTenantCommand("A", "A", null, "53200", "1234567890")))
                .containsEntry("phone", IdentityErrorCodes.INVALID_IDENTIFIER);
    }

    @Test
    void seedUsersGetTheRolesAskedFor() {
        ProvisionedAccount a =
                provision.handle(new ProvisionTenantCommand("A Koleji", "A", "a@a.test", null, "Ornek-Sifre-2026"));

        var userId = seed.handle(new CreateSeedUserCommand(
                a.tenantId(),
                "Öğretmen Veli",
                "ov@a.test",
                null,
                "Ornek-Sifre-2026",
                Set.of(Role.TEACHER, Role.GUARDIAN)));

        assertThat(fx.users.get(userId).roles()).containsExactlyInAnyOrder(Role.TEACHER, Role.GUARDIAN);
        assertThat(fx.users.get(userId).tenantId()).isEqualTo(a.tenantId());
    }
}
