package com.schoolos.core.crosscutting.exceptions;

import java.util.Map;
import java.util.TreeMap;

/** Input failed validation. Carries field → stable error code; the client builds the text (docs/conventions.md). */
public class ValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public static final ErrorCode CODE = new ErrorCode("VALIDATION_FAILED");

    @SuppressWarnings("serial") // an immutable TreeMap copy of strings
    private final Map<String, String> fieldErrors;

    public ValidationException(Map<String, String> fieldErrors) {
        super("Validation failed: " + fieldErrors.keySet());
        this.fieldErrors = Map.copyOf(new TreeMap<>(fieldErrors));
    }

    public Map<String, String> fieldErrors() {
        return fieldErrors;
    }
}
