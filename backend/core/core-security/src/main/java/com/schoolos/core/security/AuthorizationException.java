package com.schoolos.core.security;

import com.schoolos.core.crosscutting.exceptions.ErrorCode;

/** A signed-in user asked for something they may not do (HTTP 403). */
public class AuthorizationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public static final ErrorCode CODE = new ErrorCode("AUTH_FORBIDDEN");

    public AuthorizationException(String message) {
        super(message);
    }
}
