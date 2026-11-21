package com.schoolos.core.application.events;

/**
 * Publishes a domain event to every handler of its type, synchronously and inside the publisher's transaction
 * (ADR 0006): a handler that fails rolls the whole request back. No queue, no retry.
 */
@FunctionalInterface
public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
