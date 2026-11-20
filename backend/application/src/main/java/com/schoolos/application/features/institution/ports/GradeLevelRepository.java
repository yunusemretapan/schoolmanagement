package com.schoolos.application.features.institution.ports;

import com.schoolos.domain.institution.GradeLevel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Grade levels of the bound institution only (BR-1). */
public interface GradeLevelRepository {

    /** In position order. */
    List<GradeLevel> list();

    Optional<GradeLevel> findById(UUID id);

    boolean nameTaken(String name, UUID exceptId);

    void create(GradeLevel level);

    /** Writes the given levels' names and positions in one go (a move swaps two positions). */
    void update(List<GradeLevel> levels);

    void delete(UUID id);
}
