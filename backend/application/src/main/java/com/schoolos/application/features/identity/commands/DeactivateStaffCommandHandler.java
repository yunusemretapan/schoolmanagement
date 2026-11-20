package com.schoolos.application.features.identity.commands;

import com.schoolos.application.features.identity.constants.IdentityErrorCodes;
import com.schoolos.application.features.identity.dtos.StaffDetail;
import com.schoolos.application.features.identity.ports.InvitationRepository;
import com.schoolos.application.features.identity.ports.LoginIdentityRepository;
import com.schoolos.application.features.identity.ports.SessionRepository;
import com.schoolos.application.features.identity.ports.TenantLock;
import com.schoolos.application.features.identity.rules.StaffRules;
import com.schoolos.core.application.mediator.RequestHandler;
import com.schoolos.core.crosscutting.exceptions.BusinessException;
import com.schoolos.core.security.CurrentUser;
import com.schoolos.domain.identity.AccountStatus;
import com.schoolos.domain.identity.AuditAction;
import java.time.Clock;
import java.time.Instant;

/**
 * Spec 0004, Req. 8, 9 / AC-8, AC-9: never oneself, never the last active administrator (checked under the tenant
 * lock: plan 0004, R3). Every session of the account is revoked and any pending link stops working.
 */
public final class DeactivateStaffCommandHandler implements RequestHandler<DeactivateStaffCommand, StaffDetail> {

    private final LoginIdentityRepository identities;
    private final SessionRepository sessions;
    private final InvitationRepository invitations;
    private final TenantLock tenantLock;
    private final StaffRules rules;
    private final StaffAccounts staff;
    private final Clock clock;

    public DeactivateStaffCommandHandler(
            LoginIdentityRepository identities,
            SessionRepository sessions,
            InvitationRepository invitations,
            TenantLock tenantLock,
            StaffRules rules,
            StaffAccounts staff,
            Clock clock) {
        this.identities = identities;
        this.sessions = sessions;
        this.invitations = invitations;
        this.tenantLock = tenantLock;
        this.rules = rules;
        this.staff = staff;
        this.clock = clock;
    }

    @Override
    public Class<DeactivateStaffCommand> requestType() {
        return DeactivateStaffCommand.class;
    }

    @Override
    public StaffDetail handle(DeactivateStaffCommand command) {
        CurrentUser caller = StaffAccounts.caller();
        tenantLock.lock(caller.tenantId());
        StaffAccounts.Staff current = staff.loadForChange(command.staffId());
        StaffRules.ensureNotSelf(caller.userId(), current.user().id());
        if (current.identity().status() == AccountStatus.INACTIVE) {
            throw new BusinessException(IdentityErrorCodes.STAFF_STATE, "Already inactive");
        }
        rules.ensureAnotherActiveAdminRemains(current.user(), current.identity());

        Instant now = clock.instant();
        identities.update(current.identity().deactivated());
        sessions.revokeAllForUser(current.user().id(), now);
        invitations.revokeOpenForUser(current.user().id(), now);
        staff.record(caller, current.user().id(), AuditAction.STAFF_DEACTIVATED, null);
        return staff.detail(staff.load(current.user().id()), caller);
    }
}
