package com.schoolos.domain.institution;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Dönem: an academic year, whole days from {@code startsOn} to {@code endsOn}, both included (spec 0005, Req. 4).
 * Terms of one institution never share a day.
 */
public record Term(UUID id, UUID tenantId, String name, LocalDate startsOn, LocalDate endsOn, Instant createdAt) {

    public static final LocalDate EARLIEST = LocalDate.of(2000, 1, 1);
    public static final LocalDate LATEST = LocalDate.of(2100, 12, 31);
    public static final int MAX_DAYS = 731;

    /** Days the term covers, both ends included. */
    public static long lengthInDays(LocalDate startsOn, LocalDate endsOn) {
        return ChronoUnit.DAYS.between(startsOn, endsOn) + 1;
    }

    /** Whether the dates make a term: end after start, within 2000–2100, at most 731 days (AC-4). */
    public static boolean validDates(LocalDate startsOn, LocalDate endsOn) {
        return startsOn != null
                && endsOn != null
                && endsOn.isAfter(startsOn)
                && !startsOn.isBefore(EARLIEST)
                && !endsOn.isAfter(LATEST)
                && lengthInDays(startsOn, endsOn) <= MAX_DAYS;
    }

    /** Whether this term and the given days share at least one day. */
    public boolean overlaps(LocalDate otherStart, LocalDate otherEnd) {
        return !otherStart.isAfter(endsOn) && !otherEnd.isBefore(startsOn);
    }

    public Term with(String newName, LocalDate newStart, LocalDate newEnd) {
        return new Term(id, tenantId, newName, newStart, newEnd, createdAt);
    }
}
