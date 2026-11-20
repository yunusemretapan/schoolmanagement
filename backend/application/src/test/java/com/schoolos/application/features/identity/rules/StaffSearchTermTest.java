package com.schoolos.application.features.identity.rules;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Spec 0004, Req. 3 / AC-3; plan 0004, R7. */
class StaffSearchTermTest {

    private static final String PHONE = "+905321234567";

    private static boolean finds(String typed, String fullName, String email, String phone) {
        return StaffSearchTerm.parse(typed).orElseThrow().matches(fullName, email, phone);
    }

    @Test
    void blankMeansNoFilter() {
        assertThat(StaffSearchTerm.parse(null)).isEmpty();
        assertThat(StaffSearchTerm.parse("   ")).isEmpty();
    }

    @Test
    void namesAndEmailsMatchCaseInsensitivelyByPart() {
        assertThat(finds("ayşe", "Ayşe Yılmaz", null, null)).isTrue();
        assertThat(finds("YILMAZ", "Ayşe Yılmaz", null, null)).isFalse(); // Turkish dotless ı is not y-i-l-m-a-z
        assertThat(finds("YILmaz", "Ayşe YILMAZ", null, null)).isTrue();
        assertThat(finds("ORNEK.test", "Ali", "ali@ornek.test", null)).isTrue();
        assertThat(finds("veli", "Ali", "ali@ornek.test", null)).isFalse();
    }

    @Test
    void aPhoneFragmentInAnyTypedFormFindsTheNormalizedNumber() {
        assertThat(finds("0532 123", "Ali", null, PHONE)).isTrue();
        assertThat(finds("532123", "Ali", null, PHONE)).isTrue();
        assertThat(finds("+90 532 123 45 67", "Ali", null, PHONE)).isTrue();
        assertThat(finds("(0532) 123-45", "Ali", null, PHONE)).isTrue();
        assertThat(finds("0533", "Ali", null, PHONE)).isFalse();
    }

    @Test
    void fewerThanThreeDigitsDoNotSearchPhones() {
        assertThat(StaffSearchTerm.parse("05").orElseThrow().phoneDigits()).isNull();
        assertThat(finds("53", "Ali", null, PHONE)).isFalse();
    }

    @Test
    void lettersMixedWithDigitsAreTextNotAPhone() {
        assertThat(StaffSearchTerm.parse("ali532").orElseThrow().phoneDigits()).isNull();
    }

    @Test
    void overlongInputIsCut() {
        assertThat(StaffSearchTerm.parse("a".repeat(5_000)).orElseThrow().text())
                .hasSize(StaffSearchTerm.MAX_LENGTH);
    }
}
