package com.schoolos.core.crosscutting.exceptions;

/** A business rule was violated. Carries a stable code; the client builds the user-facing text from it. */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode code;

    public BusinessException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
