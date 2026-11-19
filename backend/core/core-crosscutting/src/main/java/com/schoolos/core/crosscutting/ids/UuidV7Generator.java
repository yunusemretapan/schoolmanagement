package com.schoolos.core.crosscutting.ids;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.Random;
import java.util.UUID;

/** RFC 9562 UUID version 7: 48-bit Unix milliseconds, then random bits, so ids sort by creation time. */
public final class UuidV7Generator implements IdGenerator {

    private final Clock clock;
    private final Random random;

    public UuidV7Generator(Clock clock) {
        this(clock, new SecureRandom());
    }

    public UuidV7Generator(Clock clock, Random random) {
        this.clock = clock;
        this.random = random;
    }

    @Override
    public UUID next() {
        long millis = clock.millis() & 0xFFFF_FFFF_FFFFL;
        long randA = random.nextInt(1 << 12);
        long msb = (millis << 16) | (0x7L << 12) | randA;
        long lsb = (random.nextLong() & 0x3FFF_FFFF_FFFF_FFFFL) | 0x8000_0000_0000_0000L;
        return new UUID(msb, lsb);
    }
}
