package com.schoolos.application.features.identity.rules;

import com.schoolos.application.features.identity.constants.IdentityErrorCodes;
import com.schoolos.application.features.identity.ports.InvitationRepository;
import com.schoolos.application.features.identity.ports.LoginIdentityRepository;
import com.schoolos.application.features.identity.ports.SourceRateLimiter;
import com.schoolos.core.crosscutting.exceptions.NotFoundException;
import com.schoolos.core.crosscutting.exceptions.TooManyRequestsException;
import com.schoolos.domain.identity.AccountStatus;
import com.schoolos.domain.identity.Invitation;
import com.schoolos.domain.identity.LoginIdentity;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * The open door of an invitation link (spec 0004, Req. 5, 6; G4). Per source, at most 20 tries a minute, counted apart
 * from sign-in. A malformed, unknown, wrong, expired, used or revoked link, and one whose account is no longer
 * invited, all end in the same not-found answer that reveals nothing (AC-5; plan 0004, R8).
 */
public final class InvitationGate {

    /** Bucket prefix: invitation tries never use up the sign-in allowance of the same source, nor the reverse. */
    static final String SOURCE_PREFIX = "invitation:";

    public record Opened(Invitation invitation, LoginIdentity identity) {}

    private final InvitationRepository invitations;
    private final LoginIdentityRepository identities;
    private final SourceRateLimiter rateLimiter;

    public InvitationGate(
            InvitationRepository invitations, LoginIdentityRepository identities, SourceRateLimiter rateLimiter) {
        this.invitations = invitations;
        this.identities = identities;
        this.rateLimiter = rateLimiter;
    }

    /**
     * Reads the invitation. With {@code lock}, for accepting: the account's identity row is locked first and the
     * invitation after it (the order every staff command uses), and both are checked again under the locks, so one
     * link is accepted once and a parallel change by the administrator is not overwritten (review 1, M-1).
     */
    public Opened open(String token, String source, Instant now, boolean lock) {
        if (!rateLimiter.tryAcquire(SOURCE_PREFIX + source)) {
            throw new TooManyRequestsException(IdentityErrorCodes.RATE_LIMITED);
        }
        return InvitationPolicy.parse(token)
                .flatMap(parsed -> {
                    Predicate<Invitation> valid =
                            invitation -> InvitationPolicy.accepts(invitation, parsed.secret(), now);
                    Optional<Invitation> found =
                            invitations.findById(parsed.invitationId()).filter(valid);
                    if (!lock) {
                        return found.flatMap(invitation -> identities
                                .findByUserId(invitation.userId())
                                .filter(InvitationGate::invited)
                                .map(identity -> new Opened(invitation, identity)));
                    }
                    return found.flatMap(invitation -> identities.lockByUserId(invitation.userId()))
                            .filter(InvitationGate::invited)
                            .flatMap(identity -> invitations
                                    .lockById(parsed.invitationId())
                                    .filter(valid)
                                    .map(invitation -> new Opened(invitation, identity)));
                })
                .orElseThrow(() -> new NotFoundException("Invitation"));
    }

    private static boolean invited(LoginIdentity identity) {
        return identity.status() == AccountStatus.INVITED;
    }
}
