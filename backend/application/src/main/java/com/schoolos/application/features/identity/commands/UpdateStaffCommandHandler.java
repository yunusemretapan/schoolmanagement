package com.schoolos.application.features.identity.commands;

import com.schoolos.application.features.identity.constants.IdentityErrorCodes;
import com.schoolos.application.features.identity.dtos.StaffDetail;
import com.schoolos.application.features.identity.ports.InvitationRepository;
import com.schoolos.application.features.identity.ports.LoginIdentityRepository;
import com.schoolos.application.features.identity.ports.TenantLock;
import com.schoolos.application.features.identity.ports.UserRepository;
import com.schoolos.application.features.identity.rules.IdentityBusinessRules;
import com.schoolos.application.features.identity.rules.StaffRules;
import com.schoolos.core.application.mediator.RequestHandler;
import com.schoolos.core.crosscutting.exceptions.BusinessException;
import com.schoolos.core.security.CurrentUser;
import com.schoolos.domain.identity.AccountStatus;
import com.schoolos.domain.identity.AuditAction;
import com.schoolos.domain.identity.LoginIdentity;
import com.schoolos.domain.identity.Role;
import com.schoolos.domain.identity.UserAccount;
import java.time.Clock;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Spec 0004, Req. 7, 9, 11 / AC-7, AC-9: name and roles change in any status; e-mail and phone only while invited,
 * and changing them revokes the pending link. Removing an administrator role is checked under the tenant lock
 * (plan 0004, R3). Each added and removed role, and a details change, is one audit record (Req. 10).
 */
public final class UpdateStaffCommandHandler implements RequestHandler<UpdateStaffCommand, StaffDetail> {

    private final UserRepository users;
    private final LoginIdentityRepository identities;
    private final InvitationRepository invitations;
    private final TenantLock tenantLock;
    private final IdentityBusinessRules identityRules;
    private final StaffRules rules;
    private final StaffAccounts staff;
    private final Clock clock;

    public UpdateStaffCommandHandler(
            UserRepository users,
            LoginIdentityRepository identities,
            InvitationRepository invitations,
            TenantLock tenantLock,
            StaffRules rules,
            StaffAccounts staff,
            Clock clock) {
        this.users = users;
        this.identities = identities;
        this.invitations = invitations;
        this.tenantLock = tenantLock;
        this.identityRules = new IdentityBusinessRules(identities);
        this.rules = rules;
        this.staff = staff;
        this.clock = clock;
    }

    @Override
    public Class<UpdateStaffCommand> requestType() {
        return UpdateStaffCommand.class;
    }

    @Override
    public StaffDetail handle(UpdateStaffCommand command) {
        CurrentUser caller = StaffAccounts.caller();
        tenantLock.lock(caller.tenantId());
        StaffAccounts.Staff current = staff.load(command.staffId());
        UserAccount user = current.user();
        LoginIdentity identity = current.identity();

        Set<Role> roles = EnumSet.copyOf(command.roles());
        if (user.roles().contains(Role.GUARDIAN)) {
            roles.add(Role.GUARDIAN); // kept, never changed here (spec 0004, G3)
        }
        boolean dropsAdmin = user.roles().contains(Role.ADMIN) && !roles.contains(Role.ADMIN);
        if (dropsAdmin) {
            StaffRules.ensureNotSelf(caller.userId(), user.id());
            rules.ensureAnotherActiveAdminRemains(user, identity);
        }

        String email = Identifiers.email(command.email());
        String phone = Identifiers.phone(command.phone());
        boolean emailChanged = !Objects.equals(email, identity.email());
        boolean phoneChanged = !Objects.equals(phone, identity.phone());
        if (emailChanged || phoneChanged) {
            if (identity.status() != AccountStatus.INVITED) {
                throw new BusinessException(IdentityErrorCodes.IDENTIFIER_LOCKED, "Sign-in identifiers are fixed");
            }
            identityRules.ensureIdentifiersFreeExcept(user.id(), email, phone);
        }

        String fullName = command.fullName().strip();
        boolean nameChanged = !fullName.equals(user.fullName());
        users.update(user.withFullName(fullName).withRoles(roles));
        if (emailChanged || phoneChanged) {
            identities.update(identity.withIdentifiers(email, phone));
            invitations.revokeOpenForUser(user.id(), clock.instant());
        }

        for (Role added : sortedDifference(roles, user.roles())) {
            staff.record(caller, user.id(), AuditAction.ROLE_ADDED, added.name());
        }
        for (Role removed : sortedDifference(user.roles(), roles)) {
            staff.record(caller, user.id(), AuditAction.ROLE_REMOVED, removed.name());
        }
        List<String> changedFields = new ArrayList<>();
        if (nameChanged) {
            changedFields.add("fullName");
        }
        if (emailChanged) {
            changedFields.add("email");
        }
        if (phoneChanged) {
            changedFields.add("phone");
        }
        if (!changedFields.isEmpty()) {
            staff.record(caller, user.id(), AuditAction.STAFF_DETAILS_CHANGED, String.join(",", changedFields));
        }
        return staff.detail(staff.load(user.id()), caller);
    }

    private static List<Role> sortedDifference(Set<Role> from, Set<Role> minus) {
        return from.stream().filter(role -> !minus.contains(role)).sorted().toList();
    }
}
