package com.schoolos.application.features.identity.constants;

import com.schoolos.core.crosscutting.exceptions.ErrorCode;

/** Stable codes of the identity feature (docs/conventions.md, Naming). The client builds Turkish text from them. */
public final class IdentityErrorCodes {

    /** Unknown identifier, wrong password or locked account: deliberately one code (spec 0003, Req. 4). */
    public static final ErrorCode INVALID_CREDENTIALS = new ErrorCode("IDENTITY_INVALID_CREDENTIALS");

    public static final ErrorCode SESSION_EXPIRED = new ErrorCode("IDENTITY_SESSION_EXPIRED");
    public static final ErrorCode RATE_LIMITED = new ErrorCode("IDENTITY_RATE_LIMITED");
    public static final ErrorCode IDENTIFIER_TAKEN = new ErrorCode("IDENTITY_IDENTIFIER_TAKEN");

    /** Field-level validation codes. */
    public static final String INVALID_IDENTIFIER = "INVALID_IDENTIFIER";

    public static final String IDENTIFIER_REQUIRED = "IDENTIFIER_REQUIRED";
    public static final String PASSWORD_TOO_SHORT = "PASSWORD_TOO_SHORT";

    private IdentityErrorCodes() {}
}
