package com.schoolos.core.application.behaviors;

import java.util.function.Supplier;

/** Runs work in one database transaction: commit on return, roll back on exception. Implemented in persistence. */
@FunctionalInterface
public interface TransactionPort {

    <T> T inTransaction(Supplier<T> work);
}
