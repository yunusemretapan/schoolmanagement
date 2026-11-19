package com.schoolos.core.crosscutting.ids;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidV7GeneratorTest {

    @Test
    void producesVersion7VariantRfcIdsCarryingTheTimestamp() {
        Instant now = Instant.parse("2026-11-19T10:00:00Z");
        UUID id = new UuidV7Generator(Clock.fixed(now, ZoneOffset.UTC), new Random(42)).next();

        assertThat(id.version()).isEqualTo(7);
        assertThat(id.variant()).isEqualTo(2);
        assertThat(id.getMostSignificantBits() >>> 16).isEqualTo(now.toEpochMilli());
    }

    @Test
    void laterIdsSortAfterEarlierOnes() {
        Random random = new Random(7);
        UUID first =
                new UuidV7Generator(Clock.fixed(Instant.parse("2026-11-19T10:00:00Z"), ZoneOffset.UTC), random).next();
        UUID second =
                new UuidV7Generator(Clock.fixed(Instant.parse("2026-11-19T10:00:01Z"), ZoneOffset.UTC), random).next();

        assertThat(second.toString()).isGreaterThan(first.toString());
    }
}
