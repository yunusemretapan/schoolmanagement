package com.schoolos.application.features.identity.constants;

import com.schoolos.core.crosscutting.exceptions.ErrorCode;

/** Stable codes of the identity feature (docs/conventions.md, Naming). The client builds Turkish text from them. */
public final class IdentityErrorCodes {

    /** Unknown identifier, wrong password or locked account: deliberately one code (spec 0003, Req. 4). */
    public static final ErrorCode INVALID_CREDENTIALS = new ErrorCode("IDENTITY_INVALID_CREDENTIALS");

    public static final ErrorCode SESSION_EXPIRED = new ErrorCode("IDENTITY_SESSION_EXPIRED");
    public static final ErrorCode RATE_LIMITED = new ErrorCode("IDENTITY_RATE_LIMITED");
    /** Whichever institution holds it: the answer never says where (spec 0004, AC-4; plan 0004, R8). */
    public static final ErrorCode IDENTIFIER_TAKEN = new ErrorCode("IDENTITY_IDENTIFIER_TAKEN");
    /** The institution would be left without an active administrator (spec 0004, Req. 9). */
    public static final ErrorCode LAST_ADMIN = new ErrorCode("IDENTITY_LAST_ADMIN");
    /** An administrator cannot deactivate themselves or remove their own administrator role (spec 0004, Req. 9). */
    public static final ErrorCode SELF_CHANGE = new ErrorCode("IDENTITY_SELF_CHANGE");
    /** The action does not fit the account's status (e.g. renewing the link of an active account). */
    public static final ErrorCode STAFF_STATE = new ErrorCode("IDENTITY_STAFF_STATE");
    /** E-mail and phone change only while the invitation is pending (spec 0004, Req. 11). */
    public static final ErrorCode IDENTIFIER_LOCKED = new ErrorCode("IDENTITY_IDENTIFIER_LOCKED");

    /** Field-level validation codes. */
    public static final String INVALID_IDENTIFIER = "INVALID_IDENTIFIER";

    public static final String IDENTIFIER_REQUIRED = "IDENTIFIER_REQUIRED";
    public static final String PASSWORD_TOO_SHORT = "PASSWORD_TOO_SHORT";
    public static final String ROLE_REQUIRED = "ROLE_REQUIRED";
    /** Veli is not assigned from staff management (spec 0004, Req. 4). */
    public static final String ROLE_NOT_ASSIGNABLE = "ROLE_NOT_ASSIGNABLE";

    private IdentityErrorCodes() {}
}
