package com.schoolos.core.application.events;

import java.util.List;

/**
 * Hands each event to every handler of exactly its type, in registration order, on the caller's thread and therefore
 * inside the caller's transaction (ADR 0006). An exception from a handler propagates to the publisher.
 */
public final class SynchronousDomainEventPublisher implements DomainEventPublisher {

    private final List<DomainEventHandler<?>> handlers;

    public SynchronousDomainEventPublisher(List<DomainEventHandler<?>> handlers) {
        this.handlers = List.copyOf(handlers);
    }

    @Override
    public void publish(DomainEvent event) {
        for (DomainEventHandler<?> handler : handlers) {
            if (handler.eventType() == event.getClass()) {
                deliver(handler, event);
            }
        }
    }

    private static <E extends DomainEvent> void deliver(DomainEventHandler<E> handler, DomainEvent event) {
        handler.handle(handler.eventType().cast(event));
    }
}
