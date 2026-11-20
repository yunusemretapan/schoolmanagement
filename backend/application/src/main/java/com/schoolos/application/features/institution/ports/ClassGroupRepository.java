package com.schoolos.application.features.institution.ports;

import com.schoolos.domain.institution.ClassGroup;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Class groups of the bound institution only (BR-1). */
public interface ClassGroupRepository {

    /** The term's class groups, of one campus when {@code campusId} is given. */
    List<ClassGroup> list(UUID termId, Optional<UUID> campusId);

    Optional<ClassGroup> findById(UUID id);

    /** Whether another class group (not {@code exceptId}) of the campus and term has the same folded name. */
    boolean nameTaken(UUID campusId, UUID termId, String name, UUID exceptId);

    boolean anyInCampus(UUID campusId);

    boolean anyInTerm(UUID termId);

    boolean anyOfGradeLevel(UUID gradeLevelId);

    void create(ClassGroup group);

    void update(ClassGroup group);

    void delete(UUID id);
}
