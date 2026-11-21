package com.schoolos.application.features.institution.ports;

import com.schoolos.domain.institution.Term;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Terms of the bound institution only (BR-1). */
public interface TermRepository {

    /** Newest first (by start date). */
    List<Term> list();

    Optional<Term> findById(UUID id);

    boolean nameTaken(String name, UUID exceptId);

    /** Whether another term (not {@code exceptId}) shares at least one day with these dates. */
    boolean overlapping(LocalDate startsOn, LocalDate endsOn, UUID exceptId);

    void create(Term term);

    void update(Term term);

    void delete(UUID id);
}
