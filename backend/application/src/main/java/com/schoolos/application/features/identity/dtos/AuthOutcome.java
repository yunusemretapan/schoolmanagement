package com.schoolos.application.features.identity.dtos;

import com.schoolos.core.crosscutting.exceptions.ErrorCode;
import com.schoolos.domain.identity.ClientType;

/**
 * Result of login or refresh. A refusal is a value, not an exception, so that what it records (a failed attempt, a
 * revoked chain) is committed rather than rolled back.
 */
public sealed interface AuthOutcome {

    /** Signed in: a 15-minute access token and the next refresh token for this device. */
    record Issued(String accessToken, long accessExpiresInSeconds, String refreshToken, ClientType clientType)
            implements AuthOutcome {}

    record Refused(ErrorCode code) implements AuthOutcome {}
}
