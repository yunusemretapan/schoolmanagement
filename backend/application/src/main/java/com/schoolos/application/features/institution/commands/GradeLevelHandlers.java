package com.schoolos.application.features.institution.commands;

import com.schoolos.application.features.institution.commands.GradeLevelCommands.AddStandardGradeLevels;
import com.schoolos.application.features.institution.commands.GradeLevelCommands.CreateGradeLevel;
import com.schoolos.application.features.institution.commands.GradeLevelCommands.DeleteGradeLevel;
import com.schoolos.application.features.institution.commands.GradeLevelCommands.Direction;
import com.schoolos.application.features.institution.commands.GradeLevelCommands.MoveGradeLevel;
import com.schoolos.application.features.institution.commands.GradeLevelCommands.RenameGradeLevel;
import com.schoolos.application.features.institution.constants.InstitutionErrorCodes;
import com.schoolos.application.features.institution.dtos.AcademicStructure;
import com.schoolos.application.features.institution.ports.ClassGroupRepository;
import com.schoolos.application.features.institution.ports.GradeLevelRepository;
import com.schoolos.application.features.institution.rules.AcademicStructureReader;
import com.schoolos.application.features.institution.rules.AcademicWrites;
import com.schoolos.core.application.mediator.RequestHandler;
import com.schoolos.core.crosscutting.exceptions.BusinessException;
import com.schoolos.core.crosscutting.exceptions.NotFoundException;
import com.schoolos.domain.institution.AcademicAuditAction;
import com.schoolos.domain.institution.AcademicNames;
import com.schoolos.domain.institution.AcademicRecordType;
import com.schoolos.domain.institution.GradeLevel;
import com.schoolos.domain.institution.StandardGradeLevels;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Spec 0005, Req. 5, 9: the institution's grade levels, ordered by position 1…n with no gaps, at most 50. Every write
 * locks the institution first (plan R3).
 */
public final class GradeLevelHandlers {

    private GradeLevelHandlers() {}

    static GradeLevel load(GradeLevelRepository levels, UUID id) {
        return levels.findById(id).orElseThrow(() -> new NotFoundException("GradeLevel"));
    }

    static void ensureFree(GradeLevelRepository levels, String name, UUID exceptId) {
        if (levels.nameTaken(name, exceptId)) {
            throw new BusinessException(InstitutionErrorCodes.NAME_TAKEN, "Grade level name in use");
        }
    }

    public static final class Create implements RequestHandler<CreateGradeLevel, AcademicStructure> {

        private final GradeLevelRepository levels;
        private final AcademicWrites writes;
        private final AcademicStructureReader reader;

        public Create(GradeLevelRepository levels, AcademicWrites writes, AcademicStructureReader reader) {
            this.levels = levels;
            this.writes = writes;
            this.reader = reader;
        }

        @Override
        public Class<CreateGradeLevel> requestType() {
            return CreateGradeLevel.class;
        }

        @Override
        public AcademicStructure handle(CreateGradeLevel command) {
            String name = AcademicNames.normalize(command.name());
            writes.lock();
            int count = levels.list().size();
            if (count >= GradeLevel.MAX_PER_INSTITUTION) {
                throw new BusinessException(InstitutionErrorCodes.GRADE_LEVEL_LIMIT, "At most 50 grade levels");
            }
            ensureFree(levels, name, null);
            GradeLevel level =
                    new GradeLevel(writes.newId(), AcademicWrites.caller().tenantId(), name, count + 1, writes.now());
            levels.create(level);
            writes.record(AcademicRecordType.GRADE_LEVEL, level.id(), name, AcademicAuditAction.GRADE_LEVEL_CREATED);
            return reader.read();
        }
    }

    public static final class Rename implements RequestHandler<RenameGradeLevel, AcademicStructure> {

        private final GradeLevelRepository levels;
        private final AcademicWrites writes;
        private final AcademicStructureReader reader;

        public Rename(GradeLevelRepository levels, AcademicWrites writes, AcademicStructureReader reader) {
            this.levels = levels;
            this.writes = writes;
            this.reader = reader;
        }

        @Override
        public Class<RenameGradeLevel> requestType() {
            return RenameGradeLevel.class;
        }

        @Override
        public AcademicStructure handle(RenameGradeLevel command) {
            String name = AcademicNames.normalize(command.name());
            writes.lock();
            GradeLevel level = load(levels, command.gradeLevelId());
            if (!level.name().equals(name)) {
                ensureFree(levels, name, level.id());
                levels.update(List.of(level.withName(name)));
                writes.record(
                        AcademicRecordType.GRADE_LEVEL, level.id(), name, AcademicAuditAction.GRADE_LEVEL_RENAMED);
            }
            return reader.read();
        }
    }

    public static final class Move implements RequestHandler<MoveGradeLevel, AcademicStructure> {

        private final GradeLevelRepository levels;
        private final AcademicWrites writes;
        private final AcademicStructureReader reader;

        public Move(GradeLevelRepository levels, AcademicWrites writes, AcademicStructureReader reader) {
            this.levels = levels;
            this.writes = writes;
            this.reader = reader;
        }

        @Override
        public Class<MoveGradeLevel> requestType() {
            return MoveGradeLevel.class;
        }

        @Override
        public AcademicStructure handle(MoveGradeLevel command) {
            writes.lock();
            List<GradeLevel> all = levels.list();
            int index = indexOf(all, command.gradeLevelId());
            int other = command.direction() == Direction.UP ? index - 1 : index + 1;
            if (other < 0 || other >= all.size()) {
                throw new BusinessException(InstitutionErrorCodes.CANNOT_MOVE, "Already at the end");
            }
            GradeLevel moved = all.get(index);
            GradeLevel swapped = all.get(other);
            levels.update(List.of(moved.at(swapped.position()), swapped.at(moved.position())));
            writes.record(
                    AcademicRecordType.GRADE_LEVEL,
                    moved.id(),
                    moved.name(),
                    AcademicAuditAction.GRADE_LEVEL_MOVED,
                    List.of("position"));
            return reader.read();
        }

        private static int indexOf(List<GradeLevel> all, UUID id) {
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).id().equals(id)) {
                    return i;
                }
            }
            throw new NotFoundException("GradeLevel");
        }
    }

    public static final class Delete implements RequestHandler<DeleteGradeLevel, AcademicStructure> {

        private final GradeLevelRepository levels;
        private final ClassGroupRepository classGroups;
        private final AcademicWrites writes;
        private final AcademicStructureReader reader;

        public Delete(
                GradeLevelRepository levels,
                ClassGroupRepository classGroups,
                AcademicWrites writes,
                AcademicStructureReader reader) {
            this.levels = levels;
            this.classGroups = classGroups;
            this.writes = writes;
            this.reader = reader;
        }

        @Override
        public Class<DeleteGradeLevel> requestType() {
            return DeleteGradeLevel.class;
        }

        @Override
        public AcademicStructure handle(DeleteGradeLevel command) {
            writes.lock();
            GradeLevel level = load(levels, command.gradeLevelId());
            if (classGroups.anyOfGradeLevel(level.id())) {
                throw new BusinessException(InstitutionErrorCodes.IN_USE, "Class groups belong to the grade level");
            }
            levels.delete(level.id());
            // close the gap: the levels after it move up one place
            List<GradeLevel> after = new ArrayList<>();
            for (GradeLevel other : levels.list()) {
                if (other.position() > level.position()) {
                    after.add(other.at(other.position() - 1));
                }
            }
            levels.update(after);
            writes.record(
                    AcademicRecordType.GRADE_LEVEL, level.id(), level.name(), AcademicAuditAction.GRADE_LEVEL_DELETED);
            return reader.read();
        }
    }

    public static final class AddStandard implements RequestHandler<AddStandardGradeLevels, AcademicStructure> {

        private final GradeLevelRepository levels;
        private final AcademicWrites writes;
        private final AcademicStructureReader reader;

        public AddStandard(GradeLevelRepository levels, AcademicWrites writes, AcademicStructureReader reader) {
            this.levels = levels;
            this.writes = writes;
            this.reader = reader;
        }

        @Override
        public Class<AddStandardGradeLevels> requestType() {
            return AddStandardGradeLevels.class;
        }

        @Override
        public AcademicStructure handle(AddStandardGradeLevels command) {
            writes.lock();
            if (!levels.list().isEmpty()) {
                throw new BusinessException(
                        InstitutionErrorCodes.GRADE_LEVELS_NOT_EMPTY, "The standard list needs an empty list");
            }
            UUID tenant = AcademicWrites.caller().tenantId();
            int position = 1;
            for (String name : StandardGradeLevels.NAMES) {
                GradeLevel level = new GradeLevel(writes.newId(), tenant, name, position++, writes.now());
                levels.create(level);
                writes.record(
                        AcademicRecordType.GRADE_LEVEL, level.id(), name, AcademicAuditAction.GRADE_LEVEL_CREATED);
            }
            return reader.read();
        }
    }
}
