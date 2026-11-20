package com.schoolos.application.features.institution.dtos;

import java.util.UUID;

/** One class group (spec 0005, Req. 6); names of its campus, term and grade level come from the structure. */
public record ClassGroupView(UUID id, String name, UUID campusId, UUID termId, UUID gradeLevelId) {}
