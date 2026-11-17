package com.schoolos.core.application.mediator;

import java.util.function.Supplier;

/**
 * A step every request passes through before its handler (Authorization, Validation, Transaction, Audit...).
 * Behaviors run in the order they are given to the {@link Mediator}; each calls {@code next} to continue.
 */
public interface PipelineBehavior {

    <R> R handle(Request<R> request, Supplier<R> next);
}
