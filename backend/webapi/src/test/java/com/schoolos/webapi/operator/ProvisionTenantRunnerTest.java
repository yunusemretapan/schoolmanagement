package com.schoolos.webapi.operator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.schoolos.application.features.identity.commands.ProvisionTenantCommand;
import com.schoolos.application.features.identity.dtos.ProvisionedAccount;
import com.schoolos.core.application.mediator.Mediator;
import com.schoolos.core.security.CurrentUserHolder;
import com.schoolos.core.security.Permission;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

/** Spec 0003, AC-1: the operator tool, with the password from the environment, never from an argument. */
class ProvisionTenantRunnerTest {

    private final Mediator mediator = mock(Mediator.class);
    private final MockEnvironment environment =
            new MockEnvironment().withProperty(ProvisionTenantRunner.PASSWORD_VARIABLE, "Kurucu-Sifre-2026");
    private final ProvisionTenantRunner runner = new ProvisionTenantRunner(mediator, environment);

    @Test
    void doesNothingWithoutItsTrigger() {
        runner.run(new DefaultApplicationArguments("--server.port=8080"));

        verify(mediator, never()).send(any());
    }

    @Test
    void provisionsAsTheOperatorWithThePasswordFromTheEnvironment() {
        AtomicReference<ProvisionTenantCommand> sent = new AtomicReference<>();
        AtomicBoolean operator = new AtomicBoolean();
        when(mediator.send(any(ProvisionTenantCommand.class))).thenAnswer(invocation -> {
            sent.set(invocation.getArgument(0));
            operator.set(CurrentUserHolder.current()
                    .map(u -> u.has(Permission.PLATFORM_OPERATOR))
                    .orElse(false));
            return new ProvisionedAccount(UUID.randomUUID(), UUID.randomUUID());
        });

        runner.run(new DefaultApplicationArguments(
                "--provision-tenant",
                "--institution-name=Örnek Koleji",
                "--founder-name=Kurucu Bey",
                "--email=kurucu@ornek.com.tr"));

        assertThat(operator).isTrue();
        assertThat(sent.get().institutionName()).isEqualTo("Örnek Koleji");
        assertThat(sent.get().email()).isEqualTo("kurucu@ornek.com.tr");
        assertThat(sent.get().phone()).isNull();
        assertThat(sent.get().password()).isEqualTo("Kurucu-Sifre-2026");
        assertThat(CurrentUserHolder.current()).isEmpty();
    }
}
