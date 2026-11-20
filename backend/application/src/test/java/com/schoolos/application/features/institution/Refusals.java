package com.schoolos.application.features.institution;

import com.schoolos.core.crosscutting.exceptions.BusinessException;
import com.schoolos.core.crosscutting.exceptions.ErrorCode;

/** The business refusal an action ends in; a test that expects one fails when the action succeeds. */
public final class Refusals {

    private Refusals() {}

    public static ErrorCode code(Runnable action) {
        try {
            action.run();
        } catch (BusinessException e) {
            return e.code();
        }
        throw new AssertionError("expected a refusal");
    }
}
