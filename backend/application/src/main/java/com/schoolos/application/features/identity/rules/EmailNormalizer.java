package com.schoolos.application.features.identity.rules;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * E-mail is case-insensitive (spec 0003, Req. 2): trimmed and lower-cased with {@link Locale#ROOT} (Turkish İ/I).
 * Length is bounded before the pattern runs, and the pattern has no overlapping repetitions (domain labels are
 * separated by mandatory dots), so matching stays linear in the input (review 1, H-2).
 */
public final class EmailNormalizer {

    /** RFC 5321 path limit. */
    public static final int MAX_LENGTH = 254;

    private static final Pattern SHAPE = Pattern.compile("^[^@\\s]+@[^@\\s.]+(?:\\.[^@\\s.]+)+$");

    private EmailNormalizer() {}

    public static Optional<String> normalize(String raw) {
        if (raw == null || raw.length() > MAX_LENGTH) {
            return Optional.empty();
        }
        String email = raw.strip().toLowerCase(Locale.ROOT);
        return SHAPE.matcher(email).matches() ? Optional.of(email) : Optional.empty();
    }
}
