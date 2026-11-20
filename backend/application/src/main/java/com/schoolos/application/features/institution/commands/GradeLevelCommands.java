package com.schoolos.application.features.institution.commands;

import com.schoolos.application.features.institution.dtos.AcademicStructure;
import com.schoolos.core.application.mediator.Request;
import com.schoolos.core.security.Permission;
import com.schoolos.core.security.RequiresPermission;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Grade level commands (spec 0005, Req. 5, 9). Each answers with the institution's structure after the change. */
public final class GradeLevelCommands {

    private GradeLevelCommands() {}

    public enum Direction {
        UP,
        DOWN
    }

    @RequiresPermission(Permission.ACADEMIC_STRUCTURE_MANAGE)
    public record CreateGradeLevel(String name) implements Request<AcademicStructure> {}

    @RequiresPermission(Permission.ACADEMIC_STRUCTURE_MANAGE)
    public record RenameGradeLevel(@NotNull UUID gradeLevelId, String name) implements Request<AcademicStructure> {}

    @RequiresPermission(Permission.ACADEMIC_STRUCTURE_MANAGE)
    public record MoveGradeLevel(
            @NotNull UUID gradeLevelId, @NotNull Direction direction) implements Request<AcademicStructure> {}

    @RequiresPermission(Permission.ACADEMIC_STRUCTURE_MANAGE)
    public record DeleteGradeLevel(@NotNull UUID gradeLevelId) implements Request<AcademicStructure> {}

    /** The 14 standard levels, only while the institution has none (Req. 5). */
    @RequiresPermission(Permission.ACADEMIC_STRUCTURE_MANAGE)
    public record AddStandardGradeLevels() implements Request<AcademicStructure> {}
}
