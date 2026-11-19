package com.schoolos.core.crosscutting.ids;

import java.util.UUID;

/** Source of new entity identifiers (UUID v7, docs/conventions.md, Data rules). Injected so tests stay deterministic. */
@FunctionalInterface
public interface IdGenerator {

    UUID next();
}
