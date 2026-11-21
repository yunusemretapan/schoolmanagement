package com.schoolos.application.features.institution.commands;

import com.schoolos.application.features.institution.dtos.ClassGroupView;
import com.schoolos.core.application.mediator.Request;
import com.schoolos.core.security.Permission;
import com.schoolos.core.security.RequiresPermission;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Class group commands (spec 0005, Req. 6, 9). */
public final class ClassGroupCommands {

    private ClassGroupCommands() {}

    /**
     * {@code campusId} may be left out while the institution has one campus; {@code termId} defaults to the active
     * term.
     */
    @RequiresPermission(Permission.ACADEMIC_STRUCTURE_MANAGE)
    public record CreateClassGroup(String name, @NotNull UUID gradeLevelId, UUID campusId, UUID termId)
            implements Request<ClassGroupView> {}

    /** Name and grade level only: a class group's campus and term never change (Req. 6). */
    @RequiresPermission(Permission.ACADEMIC_STRUCTURE_MANAGE)
    public record UpdateClassGroup(
            @NotNull UUID classGroupId,
            String name,
            @NotNull UUID gradeLevelId) implements Request<ClassGroupView> {}

    @RequiresPermission(Permission.ACADEMIC_STRUCTURE_MANAGE)
    public record DeleteClassGroup(@NotNull UUID classGroupId) implements Request<Void> {}
}
