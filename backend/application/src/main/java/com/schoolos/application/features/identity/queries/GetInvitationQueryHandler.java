package com.schoolos.application.features.identity.queries;

import com.schoolos.application.features.identity.dtos.InvitationInfo;
import com.schoolos.application.features.identity.ports.TenantRepository;
import com.schoolos.application.features.identity.ports.UserRepository;
import com.schoolos.application.features.identity.rules.InvitationGate;
import com.schoolos.core.application.behaviors.TenantSessionPort;
import com.schoolos.core.application.mediator.RequestHandler;
import com.schoolos.core.crosscutting.exceptions.NotFoundException;
import com.schoolos.domain.identity.Tenant;
import com.schoolos.domain.identity.UserAccount;
import java.time.Clock;

/** Spec 0004, Req. 5, 6 / AC-5, AC-6: only a usable link shows a name; the tenant binds only after it checked out. */
public final class GetInvitationQueryHandler implements RequestHandler<GetInvitationQuery, InvitationInfo> {

    private final InvitationGate gate;
    private final UserRepository users;
    private final TenantRepository tenants;
    private final TenantSessionPort tenantSession;
    private final Clock clock;

    public GetInvitationQueryHandler(
            InvitationGate gate,
            UserRepository users,
            TenantRepository tenants,
            TenantSessionPort tenantSession,
            Clock clock) {
        this.gate = gate;
        this.users = users;
        this.tenants = tenants;
        this.tenantSession = tenantSession;
        this.clock = clock;
    }

    @Override
    public Class<GetInvitationQuery> requestType() {
        return GetInvitationQuery.class;
    }

    @Override
    public InvitationInfo handle(GetInvitationQuery query) {
        InvitationGate.Opened opened = gate.open(query.token(), query.source(), clock.instant(), false);
        tenantSession.bind(opened.invitation().tenantId());
        UserAccount user =
                users.findById(opened.invitation().userId()).orElseThrow(() -> new NotFoundException("Invitation"));
        Tenant tenant = tenants.findById(user.tenantId()).orElseThrow(() -> new NotFoundException("Invitation"));
        return new InvitationInfo(user.fullName(), tenant.name());
    }
}
