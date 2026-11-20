package com.schoolos.webapi.config;

import com.schoolos.application.features.institution.commands.CampusCommands;
import com.schoolos.application.features.institution.commands.CampusHandlers;
import com.schoolos.application.features.institution.commands.ClassGroupCommands;
import com.schoolos.application.features.institution.commands.ClassGroupHandlers;
import com.schoolos.application.features.institution.commands.GradeLevelCommands;
import com.schoolos.application.features.institution.commands.GradeLevelHandlers;
import com.schoolos.application.features.institution.commands.InstitutionValidators;
import com.schoolos.application.features.institution.commands.TermCommands;
import com.schoolos.application.features.institution.commands.TermHandlers;
import com.schoolos.application.features.institution.events.CreateDefaultCampusOnTenantProvisioned;
import com.schoolos.application.features.institution.ports.AcademicSettingsRepository;
import com.schoolos.application.features.institution.ports.CampusRepository;
import com.schoolos.application.features.institution.ports.ClassGroupRepository;
import com.schoolos.application.features.institution.ports.GradeLevelRepository;
import com.schoolos.application.features.institution.ports.TermRepository;
import com.schoolos.application.features.institution.queries.AcademicQueryHandlers;
import com.schoolos.application.features.institution.rules.AcademicStructureReader;
import com.schoolos.application.features.institution.rules.AcademicWrites;
import com.schoolos.core.application.audit.AuditLog;
import com.schoolos.core.application.behaviors.RequestValidator;
import com.schoolos.core.crosscutting.ids.IdGenerator;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The institution feature (spec 0005): academic structure handlers, validators and the default-campus event. */
@Configuration
class InstitutionConfig {

    @Bean
    AcademicWrites academicWrites(AcademicSettingsRepository settings, AuditLog audit, IdGenerator ids, Clock clock) {
        return new AcademicWrites(settings, audit, ids, clock);
    }

    @Bean
    AcademicStructureReader academicStructureReader(
            AcademicSettingsRepository settings,
            TermRepository terms,
            CampusRepository campuses,
            GradeLevelRepository levels,
            ClassGroupRepository groups) {
        return new AcademicStructureReader(settings, terms, campuses, levels, groups);
    }

    @Bean
    CreateDefaultCampusOnTenantProvisioned createDefaultCampusOnTenantProvisioned(
            AcademicSettingsRepository settings, CampusRepository campuses, IdGenerator ids, Clock clock) {
        return new CreateDefaultCampusOnTenantProvisioned(settings, campuses, ids, clock);
    }

    // --- queries ---

    @Bean
    AcademicQueryHandlers.Structure getAcademicStructureHandler(AcademicStructureReader reader) {
        return new AcademicQueryHandlers.Structure(reader);
    }

    @Bean
    AcademicQueryHandlers.ClassGroups listClassGroupsHandler(
            AcademicSettingsRepository settings,
            TermRepository terms,
            CampusRepository campuses,
            GradeLevelRepository levels,
            ClassGroupRepository groups) {
        return new AcademicQueryHandlers.ClassGroups(settings, terms, campuses, levels, groups);
    }

    // --- campuses ---

    @Bean
    CampusHandlers.Create createCampusHandler(
            CampusRepository campuses, AcademicWrites writes, AcademicStructureReader reader) {
        return new CampusHandlers.Create(campuses, writes, reader);
    }

    @Bean
    CampusHandlers.Rename renameCampusHandler(
            CampusRepository campuses, AcademicWrites writes, AcademicStructureReader reader) {
        return new CampusHandlers.Rename(campuses, writes, reader);
    }

    @Bean
    CampusHandlers.Delete deleteCampusHandler(
            CampusRepository campuses,
            ClassGroupRepository groups,
            AcademicWrites writes,
            AcademicStructureReader reader) {
        return new CampusHandlers.Delete(campuses, groups, writes, reader);
    }

    // --- terms ---

    @Bean
    TermHandlers.Create createTermHandler(TermRepository terms, AcademicWrites writes, AcademicStructureReader reader) {
        return new TermHandlers.Create(terms, writes, reader);
    }

    @Bean
    TermHandlers.Update updateTermHandler(TermRepository terms, AcademicWrites writes, AcademicStructureReader reader) {
        return new TermHandlers.Update(terms, writes, reader);
    }

    @Bean
    TermHandlers.Activate activateTermHandler(
            TermRepository terms, AcademicWrites writes, AcademicStructureReader reader) {
        return new TermHandlers.Activate(terms, writes, reader);
    }

    @Bean
    TermHandlers.Delete deleteTermHandler(
            TermRepository terms, ClassGroupRepository groups, AcademicWrites writes, AcademicStructureReader reader) {
        return new TermHandlers.Delete(terms, groups, writes, reader);
    }

    // --- grade levels ---

    @Bean
    GradeLevelHandlers.Create createGradeLevelHandler(
            GradeLevelRepository levels, AcademicWrites writes, AcademicStructureReader reader) {
        return new GradeLevelHandlers.Create(levels, writes, reader);
    }

    @Bean
    GradeLevelHandlers.Rename renameGradeLevelHandler(
            GradeLevelRepository levels, AcademicWrites writes, AcademicStructureReader reader) {
        return new GradeLevelHandlers.Rename(levels, writes, reader);
    }

    @Bean
    GradeLevelHandlers.Move moveGradeLevelHandler(
            GradeLevelRepository levels, AcademicWrites writes, AcademicStructureReader reader) {
        return new GradeLevelHandlers.Move(levels, writes, reader);
    }

    @Bean
    GradeLevelHandlers.Delete deleteGradeLevelHandler(
            GradeLevelRepository levels,
            ClassGroupRepository groups,
            AcademicWrites writes,
            AcademicStructureReader reader) {
        return new GradeLevelHandlers.Delete(levels, groups, writes, reader);
    }

    @Bean
    GradeLevelHandlers.AddStandard addStandardGradeLevelsHandler(
            GradeLevelRepository levels, AcademicWrites writes, AcademicStructureReader reader) {
        return new GradeLevelHandlers.AddStandard(levels, writes, reader);
    }

    // --- class groups ---

    @Bean
    ClassGroupHandlers.Create createClassGroupHandler(
            ClassGroupRepository groups,
            CampusRepository campuses,
            TermRepository terms,
            GradeLevelRepository levels,
            AcademicWrites writes) {
        return new ClassGroupHandlers.Create(groups, campuses, terms, levels, writes);
    }

    @Bean
    ClassGroupHandlers.Update updateClassGroupHandler(
            ClassGroupRepository groups, GradeLevelRepository levels, AcademicWrites writes) {
        return new ClassGroupHandlers.Update(groups, levels, writes);
    }

    @Bean
    ClassGroupHandlers.Delete deleteClassGroupHandler(ClassGroupRepository groups, AcademicWrites writes) {
        return new ClassGroupHandlers.Delete(groups, writes);
    }

    // --- validators ---

    @Bean
    RequestValidator<CampusCommands.CreateCampus> createCampusValidator() {
        return InstitutionValidators.createCampus();
    }

    @Bean
    RequestValidator<CampusCommands.RenameCampus> renameCampusValidator() {
        return InstitutionValidators.renameCampus();
    }

    @Bean
    RequestValidator<TermCommands.CreateTerm> createTermValidator() {
        return InstitutionValidators.createTerm();
    }

    @Bean
    RequestValidator<TermCommands.UpdateTerm> updateTermValidator() {
        return InstitutionValidators.updateTerm();
    }

    @Bean
    RequestValidator<GradeLevelCommands.CreateGradeLevel> createGradeLevelValidator() {
        return InstitutionValidators.createGradeLevel();
    }

    @Bean
    RequestValidator<GradeLevelCommands.RenameGradeLevel> renameGradeLevelValidator() {
        return InstitutionValidators.renameGradeLevel();
    }

    @Bean
    RequestValidator<ClassGroupCommands.CreateClassGroup> createClassGroupValidator() {
        return InstitutionValidators.createClassGroup();
    }

    @Bean
    RequestValidator<ClassGroupCommands.UpdateClassGroup> updateClassGroupValidator() {
        return InstitutionValidators.updateClassGroup();
    }
}
