package com.schoolos.application.features.identity.ports;

import com.schoolos.domain.identity.Invitation;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Invitation links (not tenant-bound, like sessions: plan 0003 R1, plan 0004 R4). Only hashes are stored. */
public interface InvitationRepository {

    Optional<Invitation> findById(UUID invitationId);

    /** Reads and locks the row, so one link cannot be accepted twice in parallel. */
    Optional<Invitation> lockById(UUID invitationId);

    /** The account's usable invitation at {@code now}, if any. */
    Optional<Invitation> findUsableForUser(UUID userId, Instant now);

    void create(Invitation invitation);

    void update(Invitation invitation);

    /** Revokes every not-yet-used, not-yet-revoked invitation of the account (spec 0004, Req. 5, 8, 11). */
    void revokeOpenForUser(UUID userId, Instant now);
}
