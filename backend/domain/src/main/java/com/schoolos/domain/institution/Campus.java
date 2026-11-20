package com.schoolos.domain.institution;

import java.time.Instant;
import java.util.UUID;

/** Şube: a physical site of the institution (spec 0005, Req. 3). Every institution has at least one. */
public record Campus(UUID id, UUID tenantId, String name, Instant createdAt) {

    public Campus withName(String newName) {
        return new Campus(id, tenantId, newName, createdAt);
    }
}
