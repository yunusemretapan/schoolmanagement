package com.schoolos.application.features.institution.commands;

import com.schoolos.application.features.institution.commands.TermCommands.ActivateTerm;
import com.schoolos.application.features.institution.commands.TermCommands.CreateTerm;
import com.schoolos.application.features.institution.commands.TermCommands.DeleteTerm;
import com.schoolos.application.features.institution.commands.TermCommands.UpdateTerm;
import com.schoolos.application.features.institution.constants.InstitutionErrorCodes;
import com.schoolos.application.features.institution.dtos.AcademicStructure;
import com.schoolos.application.features.institution.ports.ClassGroupRepository;
import com.schoolos.application.features.institution.ports.TermRepository;
import com.schoolos.application.features.institution.rules.AcademicStructureReader;
import com.schoolos.application.features.institution.rules.AcademicWrites;
import com.schoolos.core.application.mediator.RequestHandler;
import com.schoolos.core.crosscutting.exceptions.BusinessException;
import com.schoolos.core.crosscutting.exceptions.NotFoundException;
import com.schoolos.domain.institution.AcademicAuditAction;
import com.schoolos.domain.institution.AcademicNames;
import com.schoolos.domain.institution.AcademicRecordType;
import com.schoolos.domain.institution.AcademicSettings;
import com.schoolos.domain.institution.Term;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Spec 0005, Req. 4, 9: terms. No two terms of an institution share a day; the first term becomes the active one;
 * the active term cannot be deleted. Every write locks the institution first (plan R3).
 */
public final class TermHandlers {

    private TermHandlers() {}

    static Term load(TermRepository terms, UUID id) {
        return terms.findById(id).orElseThrow(() -> new NotFoundException("Term"));
    }

    static void ensureFits(TermRepository terms, String name, LocalDate start, LocalDate end, UUID exceptId) {
        if (terms.nameTaken(name, exceptId)) {
            throw new BusinessException(InstitutionErrorCodes.NAME_TAKEN, "Term name in use");
        }
        if (terms.overlapping(start, end, exceptId)) {
            throw new BusinessException(InstitutionErrorCodes.TERM_OVERLAP, "The dates overlap another term");
        }
    }

    public static final class Create implements RequestHandler<CreateTerm, AcademicStructure> {

        private final TermRepository terms;
        private final AcademicWrites writes;
        private final AcademicStructureReader reader;

        public Create(TermRepository terms, AcademicWrites writes, AcademicStructureReader reader) {
            this.terms = terms;
            this.writes = writes;
            this.reader = reader;
        }

        @Override
        public Class<CreateTerm> requestType() {
            return CreateTerm.class;
        }

        @Override
        public AcademicStructure handle(CreateTerm command) {
            String name = AcademicNames.normalize(command.name());
            AcademicSettings settings = writes.lock();
            ensureFits(terms, name, command.startsOn(), command.endsOn(), null);
            Term term = new Term(
                    writes.newId(),
                    AcademicWrites.caller().tenantId(),
                    name,
                    command.startsOn(),
                    command.endsOn(),
                    writes.now());
            terms.create(term);
            writes.record(AcademicRecordType.TERM, term.id(), name, AcademicAuditAction.TERM_CREATED);
            if (settings.activeTermId() == null) {
                writes.activate(settings, term.id());
                writes.record(AcademicRecordType.TERM, term.id(), name, AcademicAuditAction.TERM_ACTIVATED);
            }
            return reader.read();
        }
    }

    public static final class Update implements RequestHandler<UpdateTerm, AcademicStructure> {

        private final TermRepository terms;
        private final AcademicWrites writes;
        private final AcademicStructureReader reader;

        public Update(TermRepository terms, AcademicWrites writes, AcademicStructureReader reader) {
            this.terms = terms;
            this.writes = writes;
            this.reader = reader;
        }

        @Override
        public Class<UpdateTerm> requestType() {
            return UpdateTerm.class;
        }

        @Override
        public AcademicStructure handle(UpdateTerm command) {
            String name = AcademicNames.normalize(command.name());
            writes.lock();
            Term term = load(terms, command.termId());
            List<String> changed = new ArrayList<>();
            if (!term.name().equals(name)) {
                changed.add("name");
            }
            if (!Objects.equals(term.startsOn(), command.startsOn())) {
                changed.add("startsOn");
            }
            if (!Objects.equals(term.endsOn(), command.endsOn())) {
                changed.add("endsOn");
            }
            if (!changed.isEmpty()) {
                ensureFits(terms, name, command.startsOn(), command.endsOn(), term.id());
                terms.update(term.with(name, command.startsOn(), command.endsOn()));
                writes.record(AcademicRecordType.TERM, term.id(), name, AcademicAuditAction.TERM_CHANGED, changed);
            }
            return reader.read();
        }
    }

    public static final class Activate implements RequestHandler<ActivateTerm, AcademicStructure> {

        private final TermRepository terms;
        private final AcademicWrites writes;
        private final AcademicStructureReader reader;

        public Activate(TermRepository terms, AcademicWrites writes, AcademicStructureReader reader) {
            this.terms = terms;
            this.writes = writes;
            this.reader = reader;
        }

        @Override
        public Class<ActivateTerm> requestType() {
            return ActivateTerm.class;
        }

        @Override
        public AcademicStructure handle(ActivateTerm command) {
            AcademicSettings settings = writes.lock();
            Term term = load(terms, command.termId());
            if (!term.id().equals(settings.activeTermId())) {
                writes.activate(settings, term.id());
                writes.record(AcademicRecordType.TERM, term.id(), term.name(), AcademicAuditAction.TERM_ACTIVATED);
            }
            return reader.read();
        }
    }

    public static final class Delete implements RequestHandler<DeleteTerm, AcademicStructure> {

        private final TermRepository terms;
        private final ClassGroupRepository classGroups;
        private final AcademicWrites writes;
        private final AcademicStructureReader reader;

        public Delete(
                TermRepository terms,
                ClassGroupRepository classGroups,
                AcademicWrites writes,
                AcademicStructureReader reader) {
            this.terms = terms;
            this.classGroups = classGroups;
            this.writes = writes;
            this.reader = reader;
        }

        @Override
        public Class<DeleteTerm> requestType() {
            return DeleteTerm.class;
        }

        @Override
        public AcademicStructure handle(DeleteTerm command) {
            AcademicSettings settings = writes.lock();
            Term term = load(terms, command.termId());
            if (term.id().equals(settings.activeTermId())) {
                throw new BusinessException(InstitutionErrorCodes.ACTIVE_TERM, "The active term stays");
            }
            if (classGroups.anyInTerm(term.id())) {
                throw new BusinessException(InstitutionErrorCodes.IN_USE, "Class groups belong to the term");
            }
            terms.delete(term.id());
            writes.record(AcademicRecordType.TERM, term.id(), term.name(), AcademicAuditAction.TERM_DELETED);
            return reader.read();
        }
    }
}
