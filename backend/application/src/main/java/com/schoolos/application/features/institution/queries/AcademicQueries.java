package com.schoolos.application.features.institution.queries;

import com.schoolos.application.features.institution.dtos.AcademicStructure;
import com.schoolos.application.features.institution.dtos.ClassGroupList;
import com.schoolos.core.application.mediator.Request;
import com.schoolos.core.security.Permission;
import com.schoolos.core.security.RequiresPermission;
import java.util.UUID;

/** Reads of the academic structure (spec 0005): administrators only, like every write (Req. 1). */
public final class AcademicQueries {

    private AcademicQueries() {}

    @RequiresPermission(Permission.ACADEMIC_STRUCTURE_MANAGE)
    public record GetAcademicStructure() implements Request<AcademicStructure> {}

    /** {@code termId} defaults to the active term; {@code campusId} filters when given. */
    @RequiresPermission(Permission.ACADEMIC_STRUCTURE_MANAGE)
    public record ListClassGroups(UUID termId, UUID campusId) implements Request<ClassGroupList> {}
}
