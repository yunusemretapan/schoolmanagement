package com.schoolos.application.features.identity.commands;

import com.schoolos.application.features.identity.ports.LoginIdentityRepository;
import com.schoolos.application.features.identity.ports.UserRepository;
import com.schoolos.core.application.behaviors.TenantSessionPort;
import com.schoolos.core.application.mediator.RequestHandler;
import com.schoolos.core.crosscutting.ids.IdGenerator;
import com.schoolos.core.security.PasswordHasher;
import java.time.Clock;
import java.util.UUID;

public final class CreateSeedUserCommandHandler implements RequestHandler<CreateSeedUserCommand, UUID> {

    private final TenantSessionPort tenantSession;
    private final AccountFactory accounts;

    public CreateSeedUserCommandHandler(
            UserRepository users,
            LoginIdentityRepository identities,
            TenantSessionPort tenantSession,
            PasswordHasher passwords,
            IdGenerator ids,
            Clock clock) {
        this.tenantSession = tenantSession;
        this.accounts = new AccountFactory(users, identities, passwords, ids, clock);
    }

    @Override
    public Class<CreateSeedUserCommand> requestType() {
        return CreateSeedUserCommand.class;
    }

    @Override
    public UUID handle(CreateSeedUserCommand command) {
        accounts.ensureFree(command.email(), command.phone());
        tenantSession.bind(command.tenantId());
        return accounts.create(
                command.tenantId(),
                command.fullName(),
                command.email(),
                command.phone(),
                command.password(),
                command.roles());
    }
}
