package com.schoolos.application.features.institution.rules;

import com.schoolos.application.features.institution.dtos.AcademicStructure;
import com.schoolos.application.features.institution.ports.AcademicSettingsRepository;
import com.schoolos.application.features.institution.ports.CampusRepository;
import com.schoolos.application.features.institution.ports.ClassGroupRepository;
import com.schoolos.application.features.institution.ports.GradeLevelRepository;
import com.schoolos.application.features.institution.ports.TermRepository;
import com.schoolos.domain.institution.AcademicSettings;
import java.util.Set;
import java.util.UUID;

/** Reads the bound institution's whole structure (spec 0005): the query's answer and every structure command's. */
public final class AcademicStructureReader {

    private final AcademicSettingsRepository settings;
    private final TermRepository terms;
    private final CampusRepository campuses;
    private final GradeLevelRepository gradeLevels;
    private final ClassGroupRepository classGroups;

    public AcademicStructureReader(
            AcademicSettingsRepository settings,
            TermRepository terms,
            CampusRepository campuses,
            GradeLevelRepository gradeLevels,
            ClassGroupRepository classGroups) {
        this.settings = settings;
        this.terms = terms;
        this.campuses = campuses;
        this.gradeLevels = gradeLevels;
        this.classGroups = classGroups;
    }

    public AcademicStructure read() {
        UUID active = settings.findForBoundTenant()
                .map(AcademicSettings::activeTermId)
                .orElse(null);
        Set<UUID> usedTerms = classGroups.usedTermIds();
        Set<UUID> usedCampuses = classGroups.usedCampusIds();
        Set<UUID> usedLevels = classGroups.usedGradeLevelIds();
        return new AcademicStructure(
                active,
                terms.list().stream()
                        .map(t -> new AcademicStructure.TermView(
                                t.id(),
                                t.name(),
                                t.startsOn(),
                                t.endsOn(),
                                t.id().equals(active),
                                usedTerms.contains(t.id())))
                        .toList(),
                campuses.list().stream()
                        .map(c -> new AcademicStructure.CampusView(c.id(), c.name(), usedCampuses.contains(c.id())))
                        .toList(),
                gradeLevels.list().stream()
                        .map(g -> new AcademicStructure.GradeLevelView(
                                g.id(), g.name(), g.position(), usedLevels.contains(g.id())))
                        .toList());
    }
}
