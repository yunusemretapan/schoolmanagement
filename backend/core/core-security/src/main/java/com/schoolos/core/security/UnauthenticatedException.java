package com.schoolos.core.security;

import com.schoolos.core.crosscutting.exceptions.ErrorCode;

/** No signed-in user, and the request is not {@link Public} (HTTP 401). */
public class UnauthenticatedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public static final ErrorCode CODE = new ErrorCode("AUTH_UNAUTHENTICATED");

    public UnauthenticatedException(String message) {
        super(message);
    }
}
