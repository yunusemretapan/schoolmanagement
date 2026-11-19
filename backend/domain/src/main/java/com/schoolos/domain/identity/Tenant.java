package com.schoolos.domain.identity;

import java.time.Instant;
import java.util.UUID;

/** Kurum: the institution that bought the system; the boundary of data isolation (BR-1). */
public record Tenant(UUID id, String name, Instant createdAt) {}
