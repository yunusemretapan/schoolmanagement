package com.schoolos.core.security;

import java.util.Optional;
import java.util.function.Supplier;

/** Thread-bound current user for the duration of one request. Set by the web filter or the operator tool only. */
public final class CurrentUserHolder {

    private static final ThreadLocal<CurrentUser> CURRENT = new ThreadLocal<>();

    private CurrentUserHolder() {}

    public static Optional<CurrentUser> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static void set(CurrentUser user) {
        CURRENT.set(user);
    }

    public static void clear() {
        CURRENT.remove();
    }

    /** Runs {@code work} as {@code user}, restoring whatever was bound before. */
    public static <T> T runAs(CurrentUser user, Supplier<T> work) {
        CurrentUser previous = CURRENT.get();
        CURRENT.set(user);
        try {
            return work.get();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}
