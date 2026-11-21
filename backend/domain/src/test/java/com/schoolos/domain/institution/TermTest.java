package com.schoolos.domain.institution;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Spec 0005, Req. 4 / AC-4: the dates of a term. */
class TermTest {

    private static LocalDate d(String iso) {
        return LocalDate.parse(iso);
    }

    @Test
    void theEndComesAfterTheStart() {
        assertThat(Term.validDates(d("2026-09-14"), d("2027-06-26"))).isTrue();
        assertThat(Term.validDates(d("2026-09-14"), d("2026-09-14"))).isFalse();
        assertThat(Term.validDates(d("2026-09-14"), d("2026-09-13"))).isFalse();
        assertThat(Term.validDates(null, d("2026-09-13"))).isFalse();
    }

    @Test
    void atMost731DaysBothEndsIncluded() {
        LocalDate start = d("2026-09-01");
        assertThat(Term.lengthInDays(start, start.plusDays(730))).isEqualTo(731);
        assertThat(Term.validDates(start, start.plusDays(730))).isTrue();
        assertThat(Term.validDates(start, start.plusDays(731))).isFalse();
    }

    @Test
    void within2000And2100() {
        assertThat(Term.validDates(d("2000-01-01"), d("2000-06-30"))).isTrue();
        assertThat(Term.validDates(d("1999-12-31"), d("2000-06-30"))).isFalse();
        assertThat(Term.validDates(d("2100-06-01"), d("2100-12-31"))).isTrue();
        assertThat(Term.validDates(d("2100-06-01"), d("2101-01-01"))).isFalse();
    }

    @Test
    void sharingEvenOneDayOverlapsTheNextDayDoesNot() {
        Term year = new Term(
                UUID.randomUUID(), UUID.randomUUID(), "2026–2027", d("2026-09-14"), d("2027-06-26"), Instant.EPOCH);

        assertThat(year.overlaps(d("2027-06-26"), d("2027-09-01"))).isTrue();
        assertThat(year.overlaps(d("2026-01-01"), d("2026-09-14"))).isTrue();
        assertThat(year.overlaps(d("2026-10-01"), d("2026-10-02"))).isTrue();
        assertThat(year.overlaps(d("2025-01-01"), d("2027-12-31"))).isTrue();
        assertThat(year.overlaps(d("2027-06-27"), d("2028-06-26"))).isFalse();
        assertThat(year.overlaps(d("2025-09-01"), d("2026-09-13"))).isFalse();
    }
}
