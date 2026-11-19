package com.schoolos.application.features.identity.rules;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Turkish mobile numbers to one form, {@code +905XXXXXXXXX} (spec 0003, Req. 2): {@code 05321234567},
 * {@code 5321234567} and {@code +90 532 123 45 67} are the same number. Spaces, dashes and parentheses are ignored;
 * anything else (letters, wrong length) is not a phone number.
 */
public final class PhoneNumberNormalizer {

    private static final Pattern SEPARATORS = Pattern.compile("[\\s\\-()]");
    private static final Pattern MOBILE = Pattern.compile("^5\\d{9}$");

    private PhoneNumberNormalizer() {}

    public static Optional<String> normalize(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String digits = SEPARATORS.matcher(raw.strip()).replaceAll("");
        String national;
        if (digits.startsWith("+90")) {
            national = digits.substring(3);
        } else if (digits.startsWith("0090")) {
            national = digits.substring(4);
        } else if (digits.startsWith("90") && digits.length() == 12) {
            national = digits.substring(2);
        } else if (digits.startsWith("0") && digits.length() == 11) {
            national = digits.substring(1);
        } else {
            national = digits;
        }
        return MOBILE.matcher(national).matches() ? Optional.of("+90" + national) : Optional.empty();
    }
}
