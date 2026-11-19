package com.schoolos.application.features.identity.ports;

/**
 * Login attempts per source, independent of the account (spec 0003, Req. 5): at most 20 per minute. Guards against
 * spreading guesses over many accounts, which the per-account lockout cannot see.
 */
@FunctionalInterface
public interface SourceRateLimiter {

    int MAX_ATTEMPTS_PER_MINUTE = 20;

    /** Counts one attempt; false when this source is over the limit. */
    boolean tryAcquire(String source);
}
