package com.schoolos.application.features.identity.rules;

import java.util.Locale;
import java.util.Optional;

/**
 * What the administrator typed into the staff search (spec 0004, Req. 3; plan 0004, R7). {@code text} matches the
 * name or e-mail case-insensitively; with three or more digits, {@code phoneDigits} also matches the stored
 * {@code +90…} number, so {@code 0532 123} finds {@code +905321234567}.
 */
public record StaffSearchTerm(String text, String phoneDigits) {

    public static final int MAX_LENGTH = 100;
    private static final int MIN_PHONE_DIGITS = 3;

    /** Blank → no filter; longer than {@link #MAX_LENGTH} is cut. */
    public static Optional<StaffSearchTerm> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String trimmed = raw.strip();
        if (trimmed.length() > MAX_LENGTH) {
            trimmed = trimmed.substring(0, MAX_LENGTH);
        }
        return Optional.of(new StaffSearchTerm(fold(trimmed), phoneDigits(trimmed)));
    }

    /**
     * Case folding both sides agree on (spec 0004 review 1, L-1): the Turkish I letters (I, İ, ı, i) all become
     * {@code i} first, then the rest is lower-cased. Java's and PostgreSQL's {@code lower} disagree on these letters
     * (PostgreSQL's depends on the collation), so the database adapter applies the same replacements in its query.
     */
    public static String fold(String value) {
        return value.replace('İ', 'i').replace('I', 'i').replace('ı', 'i').toLowerCase(Locale.ROOT);
    }

    private static String phoneDigits(String raw) {
        if (!raw.matches("[0-9+() -]+")) {
            return null;
        }
        String digits = raw.replaceAll("[^0-9]", "");
        if (raw.startsWith("+90") || (digits.startsWith("90") && digits.length() > 10)) {
            digits = digits.substring(2);
        } else if (digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        return digits.length() >= MIN_PHONE_DIGITS ? digits : null;
    }

    /** The same rule in memory (tests and small lists); the database adapter applies it in its query. */
    public boolean matches(String fullName, String email, String phone) {
        if (fullName != null && fold(fullName).contains(text)) {
            return true;
        }
        if (email != null && email.contains(text)) {
            return true;
        }
        return phoneDigits != null && phone != null && phone.contains(phoneDigits);
    }
}
