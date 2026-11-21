package com.schoolos.core.application.events;

/** Reacts to one type of domain event, in the publisher's transaction (ADR 0006). */
public interface DomainEventHandler<E extends DomainEvent> {

    Class<E> eventType();

    void handle(E event);
}
