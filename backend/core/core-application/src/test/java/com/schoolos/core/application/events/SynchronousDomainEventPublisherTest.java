package com.schoolos.core.application.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** ADR 0006: events reach every handler of their type, synchronously; a failing handler fails the publisher. */
class SynchronousDomainEventPublisherTest {

    record Opened(String name) implements DomainEvent {}

    record Closed(String name) implements DomainEvent {}

    private static <E extends DomainEvent> DomainEventHandler<E> handler(
            Class<E> type, List<String> seen, String label) {
        return new DomainEventHandler<>() {
            @Override
            public Class<E> eventType() {
                return type;
            }

            @Override
            public void handle(E event) {
                seen.add(label + ":" + event);
            }
        };
    }

    @Test
    void everyHandlerOfTheTypeRunsInOrderAndOthersDoNot() {
        List<String> seen = new ArrayList<>();
        var publisher = new SynchronousDomainEventPublisher(List.of(
                handler(Opened.class, seen, "a"), handler(Closed.class, seen, "x"), handler(Opened.class, seen, "b")));

        publisher.publish(new Opened("kurum"));

        assertThat(seen).containsExactly("a:Opened[name=kurum]", "b:Opened[name=kurum]");
    }

    @Test
    void aFailingHandlerFailsThePublisherSoTheTransactionRollsBack() {
        DomainEventHandler<Opened> failing = new DomainEventHandler<>() {
            @Override
            public Class<Opened> eventType() {
                return Opened.class;
            }

            @Override
            public void handle(Opened event) {
                throw new IllegalStateException("campus refused");
            }
        };

        assertThatThrownBy(() -> new SynchronousDomainEventPublisher(List.of(failing)).publish(new Opened("kurum")))
                .hasMessage("campus refused");
    }

    @Test
    void anEventWithoutHandlersIsFine() {
        new SynchronousDomainEventPublisher(List.of()).publish(new Closed("kurum"));
    }
}
