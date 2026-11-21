package com.schoolos.application.features.institution.queries;

import com.schoolos.application.features.institution.dtos.AcademicStructure;
import com.schoolos.application.features.institution.dtos.ClassGroupList;
import com.schoolos.application.features.institution.dtos.ClassGroupView;
import com.schoolos.application.features.institution.ports.AcademicSettingsRepository;
import com.schoolos.application.features.institution.ports.CampusRepository;
import com.schoolos.application.features.institution.ports.ClassGroupRepository;
import com.schoolos.application.features.institution.ports.GradeLevelRepository;
import com.schoolos.application.features.institution.ports.TermRepository;
import com.schoolos.application.features.institution.queries.AcademicQueries.GetAcademicStructure;
import com.schoolos.application.features.institution.queries.AcademicQueries.ListClassGroups;
import com.schoolos.application.features.institution.rules.AcademicStructureReader;
import com.schoolos.core.application.mediator.RequestHandler;
import com.schoolos.core.crosscutting.exceptions.NotFoundException;
import com.schoolos.core.crosscutting.text.NameFold;
import com.schoolos.domain.institution.AcademicSettings;
import com.schoolos.domain.institution.ClassGroup;
import com.schoolos.domain.institution.GradeLevel;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Spec 0005: the structure and the class groups of a term, of the bound institution only (BR-1). */
public final class AcademicQueryHandlers {

    private AcademicQueryHandlers() {}

    public static final class Structure implements RequestHandler<GetAcademicStructure, AcademicStructure> {

        private final AcademicStructureReader reader;

        public Structure(AcademicStructureReader reader) {
            this.reader = reader;
        }

        @Override
        public Class<GetAcademicStructure> requestType() {
            return GetAcademicStructure.class;
        }

        @Override
        public AcademicStructure handle(GetAcademicStructure query) {
            return reader.read();
        }
    }

    public static final class ClassGroups implements RequestHandler<ListClassGroups, ClassGroupList> {

        private final AcademicSettingsRepository settings;
        private final TermRepository terms;
        private final CampusRepository campuses;
        private final GradeLevelRepository levels;
        private final ClassGroupRepository groups;

        public ClassGroups(
                AcademicSettingsRepository settings,
                TermRepository terms,
                CampusRepository campuses,
                GradeLevelRepository levels,
                ClassGroupRepository groups) {
            this.settings = settings;
            this.terms = terms;
            this.campuses = campuses;
            this.levels = levels;
            this.groups = groups;
        }

        @Override
        public Class<ListClassGroups> requestType() {
            return ListClassGroups.class;
        }

        /** By grade level order, then name, folded (Req. 6). */
        @Override
        public ClassGroupList handle(ListClassGroups query) {
            UUID termId = query.termId() != null
                    ? terms.findById(query.termId())
                            .orElseThrow(() -> new NotFoundException("Term"))
                            .id()
                    : settings.findForBoundTenant()
                            .map(AcademicSettings::activeTermId)
                            .orElse(null);
            if (termId == null) {
                return new ClassGroupList(null, List.of());
            }
            Optional<UUID> campusId = Optional.ofNullable(query.campusId())
                    .map(id -> campuses.findById(id)
                            .orElseThrow(() -> new NotFoundException("Campus"))
                            .id());
            Map<UUID, Integer> positions =
                    levels.list().stream().collect(Collectors.toMap(GradeLevel::id, GradeLevel::position));
            Function<ClassGroup, Integer> position = g -> positions.getOrDefault(g.gradeLevelId(), Integer.MAX_VALUE);
            List<ClassGroupView> items = groups.list(termId, campusId).stream()
                    .sorted(Comparator.comparing(position)
                            .thenComparing(g -> NameFold.fold(g.name()))
                            .thenComparing(ClassGroup::id))
                    .map(g -> new ClassGroupView(g.id(), g.name(), g.campusId(), g.termId(), g.gradeLevelId()))
                    .toList();
            return new ClassGroupList(termId, items);
        }
    }
}
