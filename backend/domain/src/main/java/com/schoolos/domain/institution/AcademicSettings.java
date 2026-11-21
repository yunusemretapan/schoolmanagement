package com.schoolos.domain.institution;

import java.util.UUID;

/**
 * One per institution (spec 0005, Req. 4; plan 0005, R3): the active term, if there is a term at all. Holding it in
 * one row makes "at most one active term" structural.
 */
public record AcademicSettings(UUID id, UUID tenantId, UUID activeTermId) {

    public AcademicSettings withActiveTerm(UUID termId) {
        return new AcademicSettings(id, tenantId, termId);
    }
}
