package com.schoolos.application.features.identity.commands;

import com.schoolos.application.features.identity.dtos.InvitationLink;
import com.schoolos.application.features.identity.dtos.StaffDetail;
import com.schoolos.application.features.identity.ports.AuditLog;
import com.schoolos.application.features.identity.ports.InvitationRepository;
import com.schoolos.application.features.identity.ports.LoginIdentityRepository;
import com.schoolos.application.features.identity.ports.SecretGenerator;
import com.schoolos.application.features.identity.ports.UserRepository;
import com.schoolos.application.features.identity.rules.InvitationPolicy;
import com.schoolos.application.features.identity.rules.StaffRules;
import com.schoolos.core.crosscutting.exceptions.NotFoundException;
import com.schoolos.core.crosscutting.ids.IdGenerator;
import com.schoolos.core.security.CurrentUser;
import com.schoolos.core.security.CurrentUserHolder;
import com.schoolos.core.security.UnauthenticatedException;
import com.schoolos.domain.identity.AuditAction;
import com.schoolos.domain.identity.AuditEvent;
import com.schoolos.domain.identity.Invitation;
import com.schoolos.domain.identity.LoginIdentity;
import com.schoolos.domain.identity.Role;
import com.schoolos.domain.identity.UserAccount;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * What every staff command and query shares (spec 0004): the caller, loading a staff member of the bound tenant,
 * writing the audit record, and the detail view. Another institution's account, an unknown id and an account without
 * a staff role all look the same: not found (AC-2).
 */
public final class StaffAccounts {

    /** One staff member: the tenant-bound account and its login identity. */
    public record Staff(UserAccount user, LoginIdentity identity) {}

    private final UserRepository users;
    private final LoginIdentityRepository identities;
    private final InvitationRepository invitations;
    private final AuditLog audit;
    private final StaffRules rules;
    private final IdGenerator ids;
    private final Clock clock;

    public StaffAccounts(
            UserRepository users,
            LoginIdentityRepository identities,
            InvitationRepository invitations,
            AuditLog audit,
            StaffRules rules,
            IdGenerator ids,
            Clock clock) {
        this.users = users;
        this.identities = identities;
        this.invitations = invitations;
        this.audit = audit;
        this.rules = rules;
        this.ids = ids;
        this.clock = clock;
    }

    public static CurrentUser caller() {
        return CurrentUserHolder.current().orElseThrow(() -> new UnauthenticatedException("No user"));
    }

    /** Reads through the tenant-bound repository, so another institution's account is simply not there. */
    public Staff load(UUID staffId) {
        UserAccount user = users.findById(staffId)
                .filter(u -> u.roles().stream().anyMatch(Role::isStaff))
                .orElseThrow(() -> new NotFoundException("Staff"));
        LoginIdentity identity = identities
                .findByUserId(user.id())
                .filter(i -> i.tenantId().equals(user.tenantId()))
                .orElseThrow(() -> new NotFoundException("Staff"));
        return new Staff(user, identity);
    }

    /**
     * Like {@link #load}, for a command that changes the account: the identity row is locked first and the account is
     * read after it, so a parallel command or invitation acceptance waits instead of being overwritten (review 1, M-1).
     * Lock order everywhere: tenant (when the last-admin rule applies), identity, invitations. The tenant boundary
     * comes first: another institution's id is refused before its global identity row is locked (review 2, L2-1).
     */
    public Staff loadForChange(UUID staffId) {
        if (!users.exists(staffId)) {
            throw new NotFoundException("Staff");
        }
        LoginIdentity identity = identities.lockByUserId(staffId).orElseThrow(() -> new NotFoundException("Staff"));
        UserAccount user = users.findById(staffId)
                .filter(u -> u.roles().stream().anyMatch(Role::isStaff))
                .filter(u -> identity.tenantId().equals(u.tenantId()))
                .orElseThrow(() -> new NotFoundException("Staff"));
        return new Staff(user, identity);
    }

    public void record(CurrentUser caller, UUID target, AuditAction action, String detail) {
        audit.append(new AuditEvent(
                ids.next(), caller.tenantId(), caller.userId(), target, action, detail, clock.instant()));
    }

    /** A new 72-hour link; any older open link of the account stops working (spec 0004, Req. 5). */
    public InvitationLink newInvitation(UserAccount user, SecretGenerator secrets) {
        Instant now = clock.instant();
        invitations.revokeOpenForUser(user.id(), now);
        String secret = secrets.newSecret();
        Invitation invitation = InvitationPolicy.issue(ids.next(), user.id(), user.tenantId(), secret, now);
        invitations.create(invitation);
        return new InvitationLink(user.id(), InvitationPolicy.token(invitation, secret), invitation.expiresAt());
    }

    public StaffDetail detail(Staff staff, CurrentUser caller) {
        return new StaffDetail(
                staff.user().id(),
                staff.user().fullName(),
                staff.identity().email(),
                staff.identity().phone(),
                staff.user().roles().stream().sorted().map(Role::name).toList(),
                staff.identity().status(),
                invitations
                        .findUsableForUser(staff.user().id(), clock.instant())
                        .map(Invitation::expiresAt)
                        .orElse(null),
                staff.user().id().equals(caller.userId()),
                rules.isLastActiveAdmin(staff.user(), staff.identity()));
    }
}
