package com.schoolos.application.features.institution.ports;

import com.schoolos.domain.institution.Campus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Campuses of the bound institution only (BR-1): another institution's campus is simply not there. */
public interface CampusRepository {

    List<Campus> list();

    Optional<Campus> findById(UUID id);

    /** Whether another campus (not {@code exceptId}) has the same folded name (NameFold). */
    boolean nameTaken(String name, UUID exceptId);

    long count();

    void create(Campus campus);

    void update(Campus campus);

    void delete(UUID id);
}
