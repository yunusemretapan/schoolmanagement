package com.schoolos.domain.institution;

import java.time.Instant;
import java.util.UUID;

/** Sınıf seviyesi: the institution's own levels, ordered by {@code position} from 1 (spec 0005, Req. 5). */
public record GradeLevel(UUID id, UUID tenantId, String name, int position, Instant createdAt) {

    public static final int MAX_PER_INSTITUTION = 50;

    public GradeLevel withName(String newName) {
        return new GradeLevel(id, tenantId, newName, position, createdAt);
    }

    public GradeLevel at(int newPosition) {
        return new GradeLevel(id, tenantId, name, newPosition, createdAt);
    }
}
