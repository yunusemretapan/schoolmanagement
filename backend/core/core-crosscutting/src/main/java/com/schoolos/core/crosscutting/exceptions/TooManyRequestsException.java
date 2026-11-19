package com.schoolos.core.crosscutting.exceptions;

/** The caller exceeded a rate limit; retry later (HTTP 429). */
public class TooManyRequestsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode code;

    public TooManyRequestsException(ErrorCode code) {
        super(code.value());
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
