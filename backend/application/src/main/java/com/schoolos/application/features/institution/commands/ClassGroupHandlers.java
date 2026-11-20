package com.schoolos.application.features.institution.commands;

import com.schoolos.application.features.institution.commands.ClassGroupCommands.CreateClassGroup;
import com.schoolos.application.features.institution.commands.ClassGroupCommands.DeleteClassGroup;
import com.schoolos.application.features.institution.commands.ClassGroupCommands.UpdateClassGroup;
import com.schoolos.application.features.institution.constants.InstitutionErrorCodes;
import com.schoolos.application.features.institution.dtos.ClassGroupView;
import com.schoolos.application.features.institution.ports.CampusRepository;
import com.schoolos.application.features.institution.ports.ClassGroupRepository;
import com.schoolos.application.features.institution.ports.GradeLevelRepository;
import com.schoolos.application.features.institution.ports.TermRepository;
import com.schoolos.application.features.institution.rules.AcademicWrites;
import com.schoolos.core.application.mediator.RequestHandler;
import com.schoolos.core.crosscutting.exceptions.BusinessException;
import com.schoolos.core.crosscutting.exceptions.NotFoundException;
import com.schoolos.core.crosscutting.exceptions.ValidationException;
import com.schoolos.domain.institution.AcademicAuditAction;
import com.schoolos.domain.institution.AcademicNames;
import com.schoolos.domain.institution.AcademicRecordType;
import com.schoolos.domain.institution.AcademicSettings;
import com.schoolos.domain.institution.Campus;
import com.schoolos.domain.institution.ClassGroup;
import com.schoolos.domain.institution.GradeLevel;
import com.schoolos.domain.institution.Term;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Spec 0005, Req. 6, 9: class groups. The campus, term and grade level are read through the bound institution, so
 * another institution's id is not found (AC-2). The name is unique within the campus and term. Every write locks the
 * institution first (plan R3).
 */
public final class ClassGroupHandlers {

    private ClassGroupHandlers() {}

    static ClassGroupView view(ClassGroup group) {
        return new ClassGroupView(group.id(), group.name(), group.campusId(), group.termId(), group.gradeLevelId());
    }

    static GradeLevel level(GradeLevelRepository levels, UUID id) {
        return levels.findById(id).orElseThrow(() -> new NotFoundException("GradeLevel"));
    }

    static void ensureFree(ClassGroupRepository groups, UUID campusId, UUID termId, String name, UUID exceptId) {
        if (groups.nameTaken(campusId, termId, name, exceptId)) {
            throw new BusinessException(InstitutionErrorCodes.NAME_TAKEN, "Class group name in use");
        }
    }

    public static final class Create implements RequestHandler<CreateClassGroup, ClassGroupView> {

        private final ClassGroupRepository groups;
        private final CampusRepository campuses;
        private final TermRepository terms;
        private final GradeLevelRepository levels;
        private final AcademicWrites writes;

        public Create(
                ClassGroupRepository groups,
                CampusRepository campuses,
                TermRepository terms,
                GradeLevelRepository levels,
                AcademicWrites writes) {
            this.groups = groups;
            this.campuses = campuses;
            this.terms = terms;
            this.levels = levels;
            this.writes = writes;
        }

        @Override
        public Class<CreateClassGroup> requestType() {
            return CreateClassGroup.class;
        }

        @Override
        public ClassGroupView handle(CreateClassGroup command) {
            String name = AcademicNames.normalize(command.name());
            AcademicSettings settings = writes.lock();
            Term term = term(settings, command.termId());
            Campus campus = campus(command.campusId());
            GradeLevel level = level(levels, command.gradeLevelId());
            ensureFree(groups, campus.id(), term.id(), name, null);
            ClassGroup group = new ClassGroup(
                    writes.newId(),
                    AcademicWrites.caller().tenantId(),
                    campus.id(),
                    term.id(),
                    level.id(),
                    name,
                    writes.now());
            groups.create(group);
            writes.record(AcademicRecordType.CLASS_GROUP, group.id(), name, AcademicAuditAction.CLASS_GROUP_CREATED);
            return view(group);
        }

        /** The given term, or the active one; a class group needs a term (Req. 6). */
        private Term term(AcademicSettings settings, UUID termId) {
            UUID id = termId != null ? termId : settings.activeTermId();
            if (id == null) {
                throw new BusinessException(InstitutionErrorCodes.NO_TERM, "Add a term first");
            }
            return terms.findById(id).orElseThrow(() -> new NotFoundException("Term"));
        }

        /** The given campus, or the only one; with several campuses the campus must be chosen (Req. 3). */
        private Campus campus(UUID campusId) {
            if (campusId != null) {
                return campuses.findById(campusId).orElseThrow(() -> new NotFoundException("Campus"));
            }
            List<Campus> all = campuses.list();
            if (all.size() != 1) {
                throw new ValidationException(Map.of("campusId", "NOT_NULL"));
            }
            return all.get(0);
        }
    }

    public static final class Update implements RequestHandler<UpdateClassGroup, ClassGroupView> {

        private final ClassGroupRepository groups;
        private final GradeLevelRepository levels;
        private final AcademicWrites writes;

        public Update(ClassGroupRepository groups, GradeLevelRepository levels, AcademicWrites writes) {
            this.groups = groups;
            this.levels = levels;
            this.writes = writes;
        }

        @Override
        public Class<UpdateClassGroup> requestType() {
            return UpdateClassGroup.class;
        }

        @Override
        public ClassGroupView handle(UpdateClassGroup command) {
            String name = AcademicNames.normalize(command.name());
            writes.lock();
            ClassGroup group =
                    groups.findById(command.classGroupId()).orElseThrow(() -> new NotFoundException("ClassGroup"));
            GradeLevel level = level(levels, command.gradeLevelId());
            List<String> changed = new ArrayList<>();
            if (!group.name().equals(name)) {
                changed.add("name");
            }
            if (!group.gradeLevelId().equals(level.id())) {
                changed.add("gradeLevel");
            }
            if (changed.isEmpty()) {
                return view(group);
            }
            ensureFree(groups, group.campusId(), group.termId(), name, group.id());
            ClassGroup updated = group.with(name, level.id());
            groups.update(updated);
            writes.record(
                    AcademicRecordType.CLASS_GROUP, group.id(), name, AcademicAuditAction.CLASS_GROUP_CHANGED, changed);
            return view(updated);
        }
    }

    public static final class Delete implements RequestHandler<DeleteClassGroup, Void> {

        private final ClassGroupRepository groups;
        private final AcademicWrites writes;

        public Delete(ClassGroupRepository groups, AcademicWrites writes) {
            this.groups = groups;
            this.writes = writes;
        }

        @Override
        public Class<DeleteClassGroup> requestType() {
            return DeleteClassGroup.class;
        }

        @Override
        public Void handle(DeleteClassGroup command) {
            writes.lock();
            ClassGroup group =
                    groups.findById(command.classGroupId()).orElseThrow(() -> new NotFoundException("ClassGroup"));
            groups.delete(group.id());
            writes.record(
                    AcademicRecordType.CLASS_GROUP, group.id(), group.name(), AcademicAuditAction.CLASS_GROUP_DELETED);
            return null;
        }
    }
}
