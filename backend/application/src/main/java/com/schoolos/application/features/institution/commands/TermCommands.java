package com.schoolos.application.features.institution.commands;

import com.schoolos.application.features.institution.dtos.AcademicStructure;
import com.schoolos.core.application.mediator.Request;
import com.schoolos.core.security.Permission;
import com.schoolos.core.security.RequiresPermission;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

/** Term commands (spec 0005, Req. 4, 9). Each answers with the institution's structure after the change. */
public final class TermCommands {

    private TermCommands() {}

    @RequiresPermission(Permission.ACADEMIC_STRUCTURE_MANAGE)
    public record CreateTerm(String name, LocalDate startsOn, LocalDate endsOn) implements Request<AcademicStructure> {}

    @RequiresPermission(Permission.ACADEMIC_STRUCTURE_MANAGE)
    public record UpdateTerm(@NotNull UUID termId, String name, LocalDate startsOn, LocalDate endsOn)
            implements Request<AcademicStructure> {}

    @RequiresPermission(Permission.ACADEMIC_STRUCTURE_MANAGE)
    public record ActivateTerm(@NotNull UUID termId) implements Request<AcademicStructure> {}

    @RequiresPermission(Permission.ACADEMIC_STRUCTURE_MANAGE)
    public record DeleteTerm(@NotNull UUID termId) implements Request<AcademicStructure> {}
}
