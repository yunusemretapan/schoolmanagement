package com.schoolos.webapi.controllers;

import com.schoolos.application.features.institution.commands.CampusCommands;
import com.schoolos.application.features.institution.commands.ClassGroupCommands;
import com.schoolos.application.features.institution.commands.GradeLevelCommands;
import com.schoolos.application.features.institution.commands.TermCommands;
import com.schoolos.application.features.institution.dtos.AcademicStructure;
import com.schoolos.application.features.institution.dtos.ClassGroupList;
import com.schoolos.application.features.institution.dtos.ClassGroupView;
import com.schoolos.application.features.institution.queries.AcademicQueries;
import com.schoolos.core.application.mediator.Mediator;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The academic structure for administrators (spec 0005). Who may call is decided in the Mediator
 * (ACADEMIC_STRUCTURE_MANAGE); another institution's record answers 404 like an unknown id (AC-2). Campus, term and
 * grade level changes answer with the whole structure, so the screen redraws from one response.
 */
@RestController
@RequestMapping("/api/v1/academic-structure")
public class AcademicStructureController {

    public record NameBody(String name) {}

    public record TermBody(String name, LocalDate startsOn, LocalDate endsOn) {}

    public record MoveBody(GradeLevelCommands.Direction direction) {}

    public record ClassGroupBody(String name, UUID gradeLevelId, UUID campusId, UUID termId) {}

    private final Mediator mediator;

    public AcademicStructureController(Mediator mediator) {
        this.mediator = mediator;
    }

    @GetMapping
    public AcademicStructure get() {
        return mediator.send(new AcademicQueries.GetAcademicStructure());
    }

    // --- campuses ---

    @PostMapping("/campuses")
    @ResponseStatus(HttpStatus.CREATED)
    public AcademicStructure createCampus(@RequestBody NameBody body) {
        return mediator.send(new CampusCommands.CreateCampus(body.name()));
    }

    @PutMapping("/campuses/{campusId}")
    public AcademicStructure renameCampus(@PathVariable UUID campusId, @RequestBody NameBody body) {
        return mediator.send(new CampusCommands.RenameCampus(campusId, body.name()));
    }

    @DeleteMapping("/campuses/{campusId}")
    public AcademicStructure deleteCampus(@PathVariable UUID campusId) {
        return mediator.send(new CampusCommands.DeleteCampus(campusId));
    }

    // --- terms ---

    @PostMapping("/terms")
    @ResponseStatus(HttpStatus.CREATED)
    public AcademicStructure createTerm(@RequestBody TermBody body) {
        return mediator.send(new TermCommands.CreateTerm(body.name(), body.startsOn(), body.endsOn()));
    }

    @PutMapping("/terms/{termId}")
    public AcademicStructure updateTerm(@PathVariable UUID termId, @RequestBody TermBody body) {
        return mediator.send(new TermCommands.UpdateTerm(termId, body.name(), body.startsOn(), body.endsOn()));
    }

    @PostMapping("/terms/{termId}/activation")
    public AcademicStructure activateTerm(@PathVariable UUID termId) {
        return mediator.send(new TermCommands.ActivateTerm(termId));
    }

    @DeleteMapping("/terms/{termId}")
    public AcademicStructure deleteTerm(@PathVariable UUID termId) {
        return mediator.send(new TermCommands.DeleteTerm(termId));
    }

    // --- grade levels ---

    @PostMapping("/grade-levels")
    @ResponseStatus(HttpStatus.CREATED)
    public AcademicStructure createGradeLevel(@RequestBody NameBody body) {
        return mediator.send(new GradeLevelCommands.CreateGradeLevel(body.name()));
    }

    @PostMapping("/grade-levels/standard")
    @ResponseStatus(HttpStatus.CREATED)
    public AcademicStructure addStandardGradeLevels() {
        return mediator.send(new GradeLevelCommands.AddStandardGradeLevels());
    }

    @PutMapping("/grade-levels/{gradeLevelId}")
    public AcademicStructure renameGradeLevel(@PathVariable UUID gradeLevelId, @RequestBody NameBody body) {
        return mediator.send(new GradeLevelCommands.RenameGradeLevel(gradeLevelId, body.name()));
    }

    @PostMapping("/grade-levels/{gradeLevelId}/move")
    public AcademicStructure moveGradeLevel(@PathVariable UUID gradeLevelId, @RequestBody MoveBody body) {
        return mediator.send(new GradeLevelCommands.MoveGradeLevel(gradeLevelId, body.direction()));
    }

    @DeleteMapping("/grade-levels/{gradeLevelId}")
    public AcademicStructure deleteGradeLevel(@PathVariable UUID gradeLevelId) {
        return mediator.send(new GradeLevelCommands.DeleteGradeLevel(gradeLevelId));
    }

    // --- class groups ---

    @GetMapping("/class-groups")
    public ClassGroupList listClassGroups(
            @RequestParam(name = "termId", required = false) UUID termId,
            @RequestParam(name = "campusId", required = false) UUID campusId) {
        return mediator.send(new AcademicQueries.ListClassGroups(termId, campusId));
    }

    @PostMapping("/class-groups")
    @ResponseStatus(HttpStatus.CREATED)
    public ClassGroupView createClassGroup(@RequestBody ClassGroupBody body) {
        return mediator.send(new ClassGroupCommands.CreateClassGroup(
                body.name(), body.gradeLevelId(), body.campusId(), body.termId()));
    }

    @PutMapping("/class-groups/{classGroupId}")
    public ClassGroupView updateClassGroup(@PathVariable UUID classGroupId, @RequestBody ClassGroupBody body) {
        return mediator.send(new ClassGroupCommands.UpdateClassGroup(classGroupId, body.name(), body.gradeLevelId()));
    }

    @DeleteMapping("/class-groups/{classGroupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteClassGroup(@PathVariable UUID classGroupId) {
        mediator.send(new ClassGroupCommands.DeleteClassGroup(classGroupId));
    }
}
