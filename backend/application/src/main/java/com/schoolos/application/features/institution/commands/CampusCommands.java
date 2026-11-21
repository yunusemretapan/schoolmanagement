package com.schoolos.application.features.institution.commands;

import com.schoolos.application.features.institution.dtos.AcademicStructure;
import com.schoolos.core.application.mediator.Request;
import com.schoolos.core.security.Permission;
import com.schoolos.core.security.RequiresPermission;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Campus commands (spec 0005, Req. 3, 9). Each answers with the institution's structure after the change. */
public final class CampusCommands {

    private CampusCommands() {}

    @RequiresPermission(Permission.ACADEMIC_STRUCTURE_MANAGE)
    public record CreateCampus(String name) implements Request<AcademicStructure> {}

    @RequiresPermission(Permission.ACADEMIC_STRUCTURE_MANAGE)
    public record RenameCampus(@NotNull UUID campusId, String name) implements Request<AcademicStructure> {}

    @RequiresPermission(Permission.ACADEMIC_STRUCTURE_MANAGE)
    public record DeleteCampus(@NotNull UUID campusId) implements Request<AcademicStructure> {}
}
