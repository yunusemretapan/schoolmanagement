package com.schoolos.core.application.events;

/**
 * Something that happened in one module that another module reacts to (docs/architecture.md, Communication rules;
 * ADR 0006). Events are plain records in the publishing module's {@code events} package.
 */
public interface DomainEvent {}
