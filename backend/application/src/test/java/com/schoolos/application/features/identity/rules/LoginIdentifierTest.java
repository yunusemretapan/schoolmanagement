package com.schoolos.application.features.identity.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeout;

import com.schoolos.application.features.identity.rules.LoginIdentifier.Kind;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** Spec 0003, Req. 2 and AC-2: e-mail is case-insensitive (with Turkish İ/I safe lower-casing). */
class LoginIdentifierTest {

    @Test
    void emailIsTrimmedAndLowerCasedWithRootLocale() {
        assertThat(LoginIdentifier.parse("  Kurucu@ORNEK.Test "))
                .contains(new LoginIdentifier(Kind.EMAIL, "kurucu@ornek.test"));
        assertThat(LoginIdentifier.parse("IDARI@ornek.test"))
                .contains(new LoginIdentifier(Kind.EMAIL, "idari@ornek.test"));
    }

    @Test
    void withoutAtSignItIsAPhoneNumber() {
        assertThat(LoginIdentifier.parse("0532 123 45 67")).contains(new LoginIdentifier(Kind.PHONE, "+905321234567"));
    }

    @Test
    void hostileLongInputIsRejectedQuicklyReview1H2() {
        String backtrackBait = "a@" + "a.".repeat(40_000) + "@";

        assertTimeout(Duration.ofMillis(500), () -> {
            assertThat(LoginIdentifier.parse(backtrackBait)).isEmpty();
            assertThat(EmailNormalizer.normalize(backtrackBait)).isEmpty();
            assertThat(PhoneNumberNormalizer.normalize("5".repeat(40_000))).isEmpty();
        });
        assertThat(LoginIdentifier.parse("a".repeat(250) + "@b.co")).isEmpty();
        assertThat(LoginIdentifier.parse("a".repeat(240) + "@ornek.test")).isPresent();
    }

    @Test
    void domainLabelsMustBeNonEmpty() {
        assertThat(LoginIdentifier.parse("veli@ornek..test")).isEmpty();
        assertThat(LoginIdentifier.parse("veli@okul.k12.tr")).isPresent();
    }

    @Test
    void malformedOrBlankIsNothing() {
        assertThat(LoginIdentifier.parse("kurucu@")).isEmpty();
        assertThat(LoginIdentifier.parse("a b@c.d")).isEmpty();
        assertThat(LoginIdentifier.parse("12345")).isEmpty();
        assertThat(LoginIdentifier.parse(" ")).isEmpty();
        assertThat(LoginIdentifier.parse(null)).isEmpty();
    }
}
