package com.schoolos.application.features.identity.commands;

import com.schoolos.application.features.identity.dtos.AcceptedInvitation;
import com.schoolos.application.features.identity.ports.AuditLog;
import com.schoolos.application.features.identity.ports.InvitationRepository;
import com.schoolos.application.features.identity.ports.LoginIdentityRepository;
import com.schoolos.application.features.identity.rules.InvitationGate;
import com.schoolos.core.application.behaviors.TenantSessionPort;
import com.schoolos.core.application.mediator.RequestHandler;
import com.schoolos.core.crosscutting.ids.IdGenerator;
import com.schoolos.core.security.PasswordHasher;
import com.schoolos.domain.identity.AuditAction;
import com.schoolos.domain.identity.AuditEvent;
import com.schoolos.domain.identity.Invitation;
import com.schoolos.domain.identity.LoginIdentity;
import java.time.Clock;
import java.time.Instant;

/**
 * Spec 0004, Req. 6 / AC-6: the link is read under a row lock, so it works once; the account becomes active with the
 * new password and signs in through the normal sign-in screen (no automatic sign-in: G2). The person is their own
 * actor in the audit record.
 */
public final class AcceptInvitationCommandHandler
        implements RequestHandler<AcceptInvitationCommand, AcceptedInvitation> {

    private final InvitationGate gate;
    private final InvitationRepository invitations;
    private final LoginIdentityRepository identities;
    private final AuditLog audit;
    private final PasswordHasher passwords;
    private final TenantSessionPort tenantSession;
    private final IdGenerator ids;
    private final Clock clock;

    public AcceptInvitationCommandHandler(
            InvitationGate gate,
            InvitationRepository invitations,
            LoginIdentityRepository identities,
            AuditLog audit,
            PasswordHasher passwords,
            TenantSessionPort tenantSession,
            IdGenerator ids,
            Clock clock) {
        this.gate = gate;
        this.invitations = invitations;
        this.identities = identities;
        this.audit = audit;
        this.passwords = passwords;
        this.tenantSession = tenantSession;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    public Class<AcceptInvitationCommand> requestType() {
        return AcceptInvitationCommand.class;
    }

    @Override
    public AcceptedInvitation handle(AcceptInvitationCommand command) {
        Instant now = clock.instant();
        InvitationGate.Opened opened = gate.open(command.token(), command.source(), now, true);
        Invitation invitation = opened.invitation();
        LoginIdentity identity = opened.identity();

        identities.update(identity.activatedWith(passwords.hash(command.password())));
        invitations.update(invitation.used(now));
        tenantSession.bind(invitation.tenantId());
        audit.append(new AuditEvent(
                ids.next(),
                invitation.tenantId(),
                invitation.userId(),
                invitation.userId(),
                AuditAction.INVITATION_ACCEPTED,
                null,
                now));
        return new AcceptedInvitation(identity.email() != null ? identity.email() : identity.phone());
    }
}
