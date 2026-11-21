package com.schoolos.application.features.identity.commands;

import com.schoolos.application.features.identity.dtos.ProvisionedAccount;
import com.schoolos.application.features.identity.events.TenantProvisioned;
import com.schoolos.application.features.identity.ports.LoginIdentityRepository;
import com.schoolos.application.features.identity.ports.TenantRepository;
import com.schoolos.application.features.identity.ports.UserRepository;
import com.schoolos.core.application.behaviors.TenantSessionPort;
import com.schoolos.core.application.events.DomainEventPublisher;
import com.schoolos.core.application.mediator.RequestHandler;
import com.schoolos.core.crosscutting.ids.IdGenerator;
import com.schoolos.core.security.PasswordHasher;
import com.schoolos.domain.identity.Role;
import com.schoolos.domain.identity.Tenant;
import java.time.Clock;
import java.util.Set;
import java.util.UUID;

/** Spec 0003, Req. 1 / AC-1: a new institution with its founder (ADMIN); announced as {@link TenantProvisioned}. */
public final class ProvisionTenantCommandHandler implements RequestHandler<ProvisionTenantCommand, ProvisionedAccount> {

    private final TenantRepository tenants;
    private final TenantSessionPort tenantSession;
    private final AccountFactory accounts;
    private final IdGenerator ids;
    private final Clock clock;
    private final DomainEventPublisher events;

    public ProvisionTenantCommandHandler(
            TenantRepository tenants,
            UserRepository users,
            LoginIdentityRepository identities,
            TenantSessionPort tenantSession,
            PasswordHasher passwords,
            IdGenerator ids,
            Clock clock,
            DomainEventPublisher events) {
        this.tenants = tenants;
        this.tenantSession = tenantSession;
        this.accounts = new AccountFactory(users, identities, passwords, ids, clock);
        this.ids = ids;
        this.clock = clock;
        this.events = events;
    }

    @Override
    public Class<ProvisionTenantCommand> requestType() {
        return ProvisionTenantCommand.class;
    }

    @Override
    public ProvisionedAccount handle(ProvisionTenantCommand command) {
        accounts.ensureFree(command.email(), command.phone());
        UUID tenantId = ids.next();
        tenantSession.bind(tenantId);
        tenants.create(new Tenant(tenantId, command.institutionName().strip(), clock.instant()));
        UUID userId = accounts.create(
                tenantId,
                command.founderName(),
                command.email(),
                command.phone(),
                command.password(),
                Set.of(Role.ADMIN));
        // In this transaction, the tenant bound: the institution's first campus comes with it (spec 0005, Req. 3)
        events.publish(new TenantProvisioned(tenantId, command.institutionName().strip()));
        return new ProvisionedAccount(tenantId, userId);
    }
}
