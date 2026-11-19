package com.schoolos.application.features.identity.rules;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/** E-mail is case-insensitive (spec 0003, Req. 2): trimmed and lower-cased with {@link Locale#ROOT} (Turkish İ/I). */
public final class EmailNormalizer {

    private static final Pattern SHAPE = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private EmailNormalizer() {}

    public static Optional<String> normalize(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String email = raw.strip().toLowerCase(Locale.ROOT);
        return SHAPE.matcher(email).matches() ? Optional.of(email) : Optional.empty();
    }
}
