package com.schoolos.domain.institution;

import java.time.Instant;
import java.util.UUID;

/**
 * Sınıf (e.g. 7/A): a group of a grade level, in one campus and one term (spec 0005, Req. 6). Its campus and term
 * never change after it is created; its name and grade level may.
 */
public record ClassGroup(
        UUID id, UUID tenantId, UUID campusId, UUID termId, UUID gradeLevelId, String name, Instant createdAt) {

    public ClassGroup with(String newName, UUID newGradeLevelId) {
        return new ClassGroup(id, tenantId, campusId, termId, newGradeLevelId, newName, createdAt);
    }
}
