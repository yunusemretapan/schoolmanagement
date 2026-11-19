package com.schoolos.webapi.dev;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.schoolos.application.features.identity.commands.CreateSeedUserCommand;
import com.schoolos.application.features.identity.commands.ProvisionTenantCommand;
import com.schoolos.application.features.identity.constants.IdentityErrorCodes;
import com.schoolos.application.features.identity.dtos.ProvisionedAccount;
import com.schoolos.core.application.mediator.Mediator;
import com.schoolos.core.crosscutting.exceptions.BusinessException;
import com.schoolos.core.security.CurrentUserHolder;
import com.schoolos.core.security.Permission;
import com.schoolos.domain.identity.Role;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.annotation.Profile;

/** Spec 0003, AC-1: sample users exist in development only, one per Faz 1 role. */
class DevSeedRunnerTest {

    private final Mediator mediator = mock(Mediator.class);
    private final DevSeedRunner runner = new DevSeedRunner(mediator, "Ornek-Sifre-2026");

    @Test
    void onlyTheDevProfileCreatesSampleUsers() {
        Profile profile = DevSeedRunner.class.getAnnotation(Profile.class);

        assertThat(profile).isNotNull();
        assertThat(profile.value()).containsExactly("dev");
    }

    @Test
    void seedsEveryFaz1RoleAndASecondInstitutionAsTheOperator() {
        List<Object> sent = new ArrayList<>();
        List<Boolean> asOperator = new ArrayList<>();
        when(mediator.send(any())).thenAnswer(invocation -> {
            sent.add(invocation.getArgument(0));
            asOperator.add(CurrentUserHolder.current()
                    .map(u -> u.has(Permission.PLATFORM_OPERATOR))
                    .orElse(false));
            Object command = invocation.getArgument(0);
            return command instanceof ProvisionTenantCommand
                    ? new ProvisionedAccount(UUID.randomUUID(), UUID.randomUUID())
                    : UUID.randomUUID();
        });

        runner.run(new DefaultApplicationArguments());

        assertThat(asOperator).containsOnly(true);
        assertThat(sent).filteredOn(ProvisionTenantCommand.class::isInstance).hasSize(2);
        assertThat(sent)
                .filteredOn(CreateSeedUserCommand.class::isInstance)
                .map(c -> ((CreateSeedUserCommand) c).roles())
                .containsExactlyInAnyOrder(
                        Set.of(Role.TEACHER),
                        Set.of(Role.GUARDIAN),
                        Set.of(Role.ACCOUNTANT),
                        Set.of(Role.TEACHER, Role.GUARDIAN));
        assertThat(CurrentUserHolder.current()).isEmpty();
    }

    @Test
    void anExistingSeedIsSkipped() {
        when(mediator.send(any(ProvisionTenantCommand.class)))
                .thenThrow(new BusinessException(IdentityErrorCodes.IDENTIFIER_TAKEN, "taken"));

        runner.run(new DefaultApplicationArguments());

        verify(mediator, never()).send(any(CreateSeedUserCommand.class));
    }
}
