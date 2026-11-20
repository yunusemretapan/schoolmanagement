package com.schoolos.application.features.identity.commands;

import com.schoolos.application.features.identity.constants.IdentityErrorCodes;
import com.schoolos.application.features.identity.dtos.StaffDetail;
import com.schoolos.application.features.identity.ports.LoginIdentityRepository;
import com.schoolos.core.application.mediator.RequestHandler;
import com.schoolos.core.crosscutting.exceptions.BusinessException;
import com.schoolos.core.security.CurrentUser;
import com.schoolos.domain.identity.AccountStatus;
import com.schoolos.domain.identity.AuditAction;

/**
 * Spec 0004, Req. 8 / AC-8: an account that had a password signs in with it again; one deactivated before accepting
 * is invited again and needs a new link (its old one was revoked at deactivation).
 */
public final class ReactivateStaffCommandHandler implements RequestHandler<ReactivateStaffCommand, StaffDetail> {

    private final LoginIdentityRepository identities;
    private final StaffAccounts staff;

    public ReactivateStaffCommandHandler(LoginIdentityRepository identities, StaffAccounts staff) {
        this.identities = identities;
        this.staff = staff;
    }

    @Override
    public Class<ReactivateStaffCommand> requestType() {
        return ReactivateStaffCommand.class;
    }

    @Override
    public StaffDetail handle(ReactivateStaffCommand command) {
        CurrentUser caller = StaffAccounts.caller();
        StaffAccounts.Staff current = staff.loadForChange(command.staffId());
        if (current.identity().status() != AccountStatus.INACTIVE) {
            throw new BusinessException(IdentityErrorCodes.STAFF_STATE, "Not inactive");
        }
        identities.update(current.identity().reactivated());
        staff.record(caller, current.user().id(), AuditAction.STAFF_REACTIVATED, null);
        return staff.detail(staff.load(current.user().id()), caller);
    }
}
