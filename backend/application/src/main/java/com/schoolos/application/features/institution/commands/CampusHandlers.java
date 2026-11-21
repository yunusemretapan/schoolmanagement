package com.schoolos.application.features.institution.commands;

import com.schoolos.application.features.institution.commands.CampusCommands.CreateCampus;
import com.schoolos.application.features.institution.commands.CampusCommands.DeleteCampus;
import com.schoolos.application.features.institution.commands.CampusCommands.RenameCampus;
import com.schoolos.application.features.institution.constants.InstitutionErrorCodes;
import com.schoolos.application.features.institution.dtos.AcademicStructure;
import com.schoolos.application.features.institution.ports.CampusRepository;
import com.schoolos.application.features.institution.ports.ClassGroupRepository;
import com.schoolos.application.features.institution.rules.AcademicStructureReader;
import com.schoolos.application.features.institution.rules.AcademicWrites;
import com.schoolos.core.application.mediator.RequestHandler;
import com.schoolos.core.crosscutting.exceptions.BusinessException;
import com.schoolos.core.crosscutting.exceptions.NotFoundException;
import com.schoolos.domain.institution.AcademicAuditAction;
import com.schoolos.domain.institution.AcademicNames;
import com.schoolos.domain.institution.AcademicRecordType;
import com.schoolos.domain.institution.Campus;
import java.util.UUID;

/**
 * Spec 0005, Req. 3, 9: campuses. Every write locks the institution first (plan R3); another institution's campus and
 * an unknown id are the same not-found (AC-2).
 */
public final class CampusHandlers {

    private CampusHandlers() {}

    static Campus load(CampusRepository campuses, UUID id) {
        return campuses.findById(id).orElseThrow(() -> new NotFoundException("Campus"));
    }

    static void ensureFree(CampusRepository campuses, String name, UUID exceptId) {
        if (campuses.nameTaken(name, exceptId)) {
            throw new BusinessException(InstitutionErrorCodes.NAME_TAKEN, "Campus name in use");
        }
    }

    public static final class Create implements RequestHandler<CreateCampus, AcademicStructure> {

        private final CampusRepository campuses;
        private final AcademicWrites writes;
        private final AcademicStructureReader reader;

        public Create(CampusRepository campuses, AcademicWrites writes, AcademicStructureReader reader) {
            this.campuses = campuses;
            this.writes = writes;
            this.reader = reader;
        }

        @Override
        public Class<CreateCampus> requestType() {
            return CreateCampus.class;
        }

        @Override
        public AcademicStructure handle(CreateCampus command) {
            String name = AcademicNames.normalize(command.name());
            writes.lock();
            ensureFree(campuses, name, null);
            Campus campus = new Campus(writes.newId(), AcademicWrites.caller().tenantId(), name, writes.now());
            campuses.create(campus);
            writes.record(AcademicRecordType.CAMPUS, campus.id(), name, AcademicAuditAction.CAMPUS_CREATED);
            return reader.read();
        }
    }

    public static final class Rename implements RequestHandler<RenameCampus, AcademicStructure> {

        private final CampusRepository campuses;
        private final AcademicWrites writes;
        private final AcademicStructureReader reader;

        public Rename(CampusRepository campuses, AcademicWrites writes, AcademicStructureReader reader) {
            this.campuses = campuses;
            this.writes = writes;
            this.reader = reader;
        }

        @Override
        public Class<RenameCampus> requestType() {
            return RenameCampus.class;
        }

        @Override
        public AcademicStructure handle(RenameCampus command) {
            String name = AcademicNames.normalize(command.name());
            writes.lock();
            Campus campus = load(campuses, command.campusId());
            if (!campus.name().equals(name)) {
                ensureFree(campuses, name, campus.id());
                campuses.update(campus.withName(name));
                writes.record(AcademicRecordType.CAMPUS, campus.id(), name, AcademicAuditAction.CAMPUS_RENAMED);
            }
            return reader.read();
        }
    }

    public static final class Delete implements RequestHandler<DeleteCampus, AcademicStructure> {

        private final CampusRepository campuses;
        private final ClassGroupRepository classGroups;
        private final AcademicWrites writes;
        private final AcademicStructureReader reader;

        public Delete(
                CampusRepository campuses,
                ClassGroupRepository classGroups,
                AcademicWrites writes,
                AcademicStructureReader reader) {
            this.campuses = campuses;
            this.classGroups = classGroups;
            this.writes = writes;
            this.reader = reader;
        }

        @Override
        public Class<DeleteCampus> requestType() {
            return DeleteCampus.class;
        }

        @Override
        public AcademicStructure handle(DeleteCampus command) {
            writes.lock();
            Campus campus = load(campuses, command.campusId());
            if (campuses.count() <= 1) {
                throw new BusinessException(InstitutionErrorCodes.LAST_CAMPUS, "The last campus stays");
            }
            if (classGroups.anyInCampus(campus.id())) {
                throw new BusinessException(InstitutionErrorCodes.IN_USE, "Class groups belong to the campus");
            }
            campuses.delete(campus.id());
            writes.record(AcademicRecordType.CAMPUS, campus.id(), campus.name(), AcademicAuditAction.CAMPUS_DELETED);
            return reader.read();
        }
    }
}
