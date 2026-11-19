package com.schoolos.application.features.identity.queries;

import com.schoolos.application.features.identity.dtos.CurrentUserResponse;
import com.schoolos.application.features.identity.ports.TenantRepository;
import com.schoolos.application.features.identity.ports.UserRepository;
import com.schoolos.core.application.mediator.RequestHandler;
import com.schoolos.core.crosscutting.exceptions.NotFoundException;
import com.schoolos.core.security.CurrentUser;
import com.schoolos.core.security.CurrentUserHolder;
import com.schoolos.core.security.UnauthenticatedException;
import com.schoolos.domain.identity.Role;
import com.schoolos.domain.identity.Tenant;
import com.schoolos.domain.identity.UserAccount;
import java.util.Comparator;

public final class GetCurrentUserQueryHandler implements RequestHandler<GetCurrentUserQuery, CurrentUserResponse> {

    private final UserRepository users;
    private final TenantRepository tenants;

    public GetCurrentUserQueryHandler(UserRepository users, TenantRepository tenants) {
        this.users = users;
        this.tenants = tenants;
    }

    @Override
    public Class<GetCurrentUserQuery> requestType() {
        return GetCurrentUserQuery.class;
    }

    @Override
    public CurrentUserResponse handle(GetCurrentUserQuery query) {
        CurrentUser caller = CurrentUserHolder.current().orElseThrow(() -> new UnauthenticatedException("No user"));
        UserAccount user = users.findById(caller.userId()).orElseThrow(() -> new NotFoundException("User"));
        Tenant tenant = tenants.findById(caller.tenantId()).orElseThrow(() -> new NotFoundException("Tenant"));
        return new CurrentUserResponse(
                user.id(),
                user.fullName(),
                tenant.name(),
                user.roles().stream()
                        .sorted(Comparator.naturalOrder())
                        .map(Role::name)
                        .toList());
    }
}
