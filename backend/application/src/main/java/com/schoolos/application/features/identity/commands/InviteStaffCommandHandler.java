package com.schoolos.application.features.identity.commands;

import com.schoolos.application.features.identity.dtos.InvitationLink;
import com.schoolos.application.features.identity.ports.LoginIdentityRepository;
import com.schoolos.application.features.identity.ports.SecretGenerator;
import com.schoolos.application.features.identity.ports.UserRepository;
import com.schoolos.application.features.identity.rules.IdentityBusinessRules;
import com.schoolos.core.application.mediator.RequestHandler;
import com.schoolos.core.crosscutting.ids.IdGenerator;
import com.schoolos.core.security.CurrentUser;
import com.schoolos.domain.identity.AuditAction;
import com.schoolos.domain.identity.LoginIdentity;
import com.schoolos.domain.identity.UserAccount;
import java.time.Clock;
import java.util.UUID;

/**
 * Spec 0004, Req. 4, 5 / AC-4: an invited account in the caller's institution and its first link. An e-mail or phone
 * in use anywhere is refused with one code, whichever institution holds it (plan 0004, R8).
 */
public final class InviteStaffCommandHandler implements RequestHandler<InviteStaffCommand, InvitationLink> {

    private final UserRepository users;
    private final LoginIdentityRepository identities;
    private final IdentityBusinessRules identityRules;
    private final StaffAccounts staff;
    private final SecretGenerator secrets;
    private final IdGenerator ids;
    private final Clock clock;

    public InviteStaffCommandHandler(
            UserRepository users,
            LoginIdentityRepository identities,
            StaffAccounts staff,
            SecretGenerator secrets,
            IdGenerator ids,
            Clock clock) {
        this.users = users;
        this.identities = identities;
        this.identityRules = new IdentityBusinessRules(identities);
        this.staff = staff;
        this.secrets = secrets;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    public Class<InviteStaffCommand> requestType() {
        return InviteStaffCommand.class;
    }

    @Override
    public InvitationLink handle(InviteStaffCommand command) {
        CurrentUser caller = StaffAccounts.caller();
        String email = Identifiers.email(command.email());
        String phone = Identifiers.phone(command.phone());
        identityRules.ensureIdentifiersFree(email, phone);
        UUID userId = ids.next();
        UserAccount user = new UserAccount(
                userId, caller.tenantId(), command.fullName().strip(), command.roles(), clock.instant());
        users.create(user);
        identities.create(LoginIdentity.invited(userId, caller.tenantId(), email, phone));
        InvitationLink link = staff.newInvitation(user, secrets);
        staff.record(caller, userId, AuditAction.STAFF_INVITED, Identifiers.roleList(command.roles()));
        return link;
    }
}
