package com.schoolos.core.crosscutting.exceptions;

/** The requested resource does not exist (or is not visible to the caller). */
public class NotFoundException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }
}
