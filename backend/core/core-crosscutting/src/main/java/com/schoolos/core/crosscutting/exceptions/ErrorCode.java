package com.schoolos.core.crosscutting.exceptions;

/** Stable, machine-readable error code: {@code <MODULE>_<STATE>} (docs/conventions.md, Naming). */
public record ErrorCode(String value) {

    public static final ErrorCode NOT_FOUND = new ErrorCode("NOT_FOUND");
    public static final ErrorCode INTERNAL_ERROR = new ErrorCode("INTERNAL_ERROR");

    public ErrorCode {
        if (value == null || !value.matches("[A-Z][A-Z0-9_]*")) {
            throw new IllegalArgumentException("Error code must be UPPER_SNAKE_CASE: " + value);
        }
    }
}
