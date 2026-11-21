package com.schoolos.application.features.institution.dtos;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * The bound institution's academic structure (spec 0005): terms newest first, campuses, grade levels in order, and the
 * active term. {@code inUse} says class groups belong to the record, so the screen can explain a refused delete.
 */
public record AcademicStructure(
        UUID activeTermId, List<TermView> terms, List<CampusView> campuses, List<GradeLevelView> gradeLevels) {

    public record TermView(UUID id, String name, LocalDate startsOn, LocalDate endsOn, boolean active, boolean inUse) {}

    public record CampusView(UUID id, String name, boolean inUse) {}

    public record GradeLevelView(UUID id, String name, int position, boolean inUse) {}
}
