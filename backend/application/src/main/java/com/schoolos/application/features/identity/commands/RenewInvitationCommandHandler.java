package com.schoolos.application.features.identity.commands;

import com.schoolos.application.features.identity.constants.IdentityErrorCodes;
import com.schoolos.application.features.identity.dtos.InvitationLink;
import com.schoolos.application.features.identity.ports.SecretGenerator;
import com.schoolos.core.application.mediator.RequestHandler;
import com.schoolos.core.crosscutting.exceptions.BusinessException;
import com.schoolos.core.security.CurrentUser;
import com.schoolos.domain.identity.AccountStatus;
import com.schoolos.domain.identity.AuditAction;

/** Spec 0004, Req. 5 / AC-5: only an invited account gets a link; the new one replaces every older one. */
public final class RenewInvitationCommandHandler implements RequestHandler<RenewInvitationCommand, InvitationLink> {

    private final StaffAccounts staff;
    private final SecretGenerator secrets;

    public RenewInvitationCommandHandler(StaffAccounts staff, SecretGenerator secrets) {
        this.staff = staff;
        this.secrets = secrets;
    }

    @Override
    public Class<RenewInvitationCommand> requestType() {
        return RenewInvitationCommand.class;
    }

    @Override
    public InvitationLink handle(RenewInvitationCommand command) {
        CurrentUser caller = StaffAccounts.caller();
        StaffAccounts.Staff current = staff.load(command.staffId());
        if (current.identity().status() != AccountStatus.INVITED) {
            throw new BusinessException(IdentityErrorCodes.STAFF_STATE, "Only invited accounts get a link");
        }
        InvitationLink link = staff.newInvitation(current.user(), secrets);
        staff.record(caller, current.user().id(), AuditAction.INVITATION_RENEWED, null);
        return link;
    }
}
