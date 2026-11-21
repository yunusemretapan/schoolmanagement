package com.schoolos.application.features.institution.commands;

import com.schoolos.application.features.institution.rules.AcademicInput;
import com.schoolos.core.application.behaviors.RequestValidator;
import com.schoolos.core.application.mediator.Request;
import com.schoolos.domain.institution.AcademicNames;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Field rules of every academic-structure command (spec 0005, Req. 4, 7 / AC-4, AC-7): names measured after trimming
 * (60, or 20 for a class group), term dates within 2000–2100, end after start, at most 731 days.
 */
public final class InstitutionValidators {

    private InstitutionValidators() {}

    public static RequestValidator<CampusCommands.CreateCampus> createCampus() {
        return names(CampusCommands.CreateCampus.class, CampusCommands.CreateCampus::name, AcademicNames.MAX_LENGTH);
    }

    public static RequestValidator<CampusCommands.RenameCampus> renameCampus() {
        return names(CampusCommands.RenameCampus.class, CampusCommands.RenameCampus::name, AcademicNames.MAX_LENGTH);
    }

    public static RequestValidator<GradeLevelCommands.CreateGradeLevel> createGradeLevel() {
        return names(
                GradeLevelCommands.CreateGradeLevel.class,
                GradeLevelCommands.CreateGradeLevel::name,
                AcademicNames.MAX_LENGTH);
    }

    public static RequestValidator<GradeLevelCommands.RenameGradeLevel> renameGradeLevel() {
        return names(
                GradeLevelCommands.RenameGradeLevel.class,
                GradeLevelCommands.RenameGradeLevel::name,
                AcademicNames.MAX_LENGTH);
    }

    public static RequestValidator<ClassGroupCommands.CreateClassGroup> createClassGroup() {
        return names(
                ClassGroupCommands.CreateClassGroup.class,
                ClassGroupCommands.CreateClassGroup::name,
                AcademicNames.CLASS_GROUP_MAX_LENGTH);
    }

    public static RequestValidator<ClassGroupCommands.UpdateClassGroup> updateClassGroup() {
        return names(
                ClassGroupCommands.UpdateClassGroup.class,
                ClassGroupCommands.UpdateClassGroup::name,
                AcademicNames.CLASS_GROUP_MAX_LENGTH);
    }

    public static RequestValidator<TermCommands.CreateTerm> createTerm() {
        return of(TermCommands.CreateTerm.class, c -> term(c.name(), c.startsOn(), c.endsOn()));
    }

    public static RequestValidator<TermCommands.UpdateTerm> updateTerm() {
        return of(TermCommands.UpdateTerm.class, c -> term(c.name(), c.startsOn(), c.endsOn()));
    }

    private static Map<String, String> term(String name, java.time.LocalDate start, java.time.LocalDate end) {
        Map<String, String> errors = new HashMap<>(AcademicInput.name("name", name, AcademicNames.MAX_LENGTH));
        errors.putAll(AcademicInput.dates(start, end));
        return errors;
    }

    private static <Q extends Request<?>> RequestValidator<Q> names(Class<Q> type, Function<Q, String> name, int max) {
        return of(type, command -> AcademicInput.name("name", name.apply(command), max));
    }

    private static <Q extends Request<?>> RequestValidator<Q> of(Class<Q> type, Function<Q, Map<String, String>> rule) {
        return new RequestValidator<>() {
            @Override
            public Class<Q> requestType() {
                return type;
            }

            @Override
            public Map<String, String> validate(Q request) {
                return rule.apply(request);
            }
        };
    }
}
