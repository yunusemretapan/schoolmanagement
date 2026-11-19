package com.schoolos.application.features.identity.rules;

import java.util.Optional;

/** What the user typed in "E-posta veya telefon": an e-mail if it holds an {@code @}, otherwise a phone number. */
public record LoginIdentifier(Kind kind, String value) {

    public enum Kind {
        EMAIL,
        PHONE
    }

    public static Optional<LoginIdentifier> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        if (raw.contains("@")) {
            return EmailNormalizer.normalize(raw).map(email -> new LoginIdentifier(Kind.EMAIL, email));
        }
        return PhoneNumberNormalizer.normalize(raw).map(phone -> new LoginIdentifier(Kind.PHONE, phone));
    }
}
