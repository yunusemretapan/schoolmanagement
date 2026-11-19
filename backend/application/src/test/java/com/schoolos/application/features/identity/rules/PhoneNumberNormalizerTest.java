package com.schoolos.application.features.identity.rules;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Spec 0003, AC-3 (server side). */
class PhoneNumberNormalizerTest {

    @ParameterizedTest
    @ValueSource(
            strings = {
                "05321234567",
                "5321234567",
                "+90 532 123 45 67",
                "+905321234567",
                "0532 123 45 67",
                "(0532) 123-45-67",
                "905321234567",
                "00905321234567"
            })
    void everySpellingOfTheSameMobileNumberNormalizesToOneForm(String raw) {
        assertThat(PhoneNumberNormalizer.normalize(raw)).contains("+905321234567");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "532123456", // 9 digits
                "53212345678", // 11 digits without leading 0
                "0532123456a",
                "abcdefghij",
                "02121234567", // landline, not mobile
                "+49 1512 3456789",
                "",
                "   "
            })
    void anythingElseIsNotAPhoneNumber(String raw) {
        assertThat(PhoneNumberNormalizer.normalize(raw)).isEmpty();
    }
}
