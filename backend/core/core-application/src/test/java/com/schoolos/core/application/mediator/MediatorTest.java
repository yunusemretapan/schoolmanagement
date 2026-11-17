package com.schoolos.core.application.mediator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class MediatorTest {

    record Ping(String text) implements Request<String> {}

    record Unhandled() implements Request<String> {}

    static final class PingHandler implements RequestHandler<Ping, String> {
        @Override
        public Class<Ping> requestType() {
            return Ping.class;
        }

        @Override
        public String handle(Ping request) {
            return "pong:" + request.text();
        }
    }

    static final class Recording implements PipelineBehavior {
        private final String name;
        private final List<String> log;

        Recording(String name, List<String> log) {
            this.name = name;
            this.log = log;
        }

        @Override
        public <R> R handle(Request<R> request, Supplier<R> next) {
            log.add(name + ":before");
            R result = next.get();
            log.add(name + ":after");
            return result;
        }
    }

    @Test
    void dispatchesToTheHandlerOfTheRequestType() {
        Mediator mediator = new Mediator(List.of(new PingHandler()), List.of());

        assertThat(mediator.send(new Ping("a"))).isEqualTo("pong:a");
    }

    @Test
    void runsBehaviorsInTheGivenOrderAroundTheHandler() {
        List<String> log = new ArrayList<>();
        Mediator mediator = new Mediator(
                List.of(new PingHandler()), List.of(new Recording("auth", log), new Recording("validation", log)));

        mediator.send(new Ping("a"));

        assertThat(log).containsExactly("auth:before", "validation:before", "validation:after", "auth:after");
    }

    @Test
    void aBehaviorCanStopTheRequestBeforeTheHandler() {
        PipelineBehavior deny = new PipelineBehavior() {
            @Override
            public <R> R handle(Request<R> request, Supplier<R> next) {
                throw new IllegalArgumentException("denied");
            }
        };
        Mediator mediator = new Mediator(List.of(new PingHandler()), List.of(deny));

        assertThatThrownBy(() -> mediator.send(new Ping("a"))).hasMessage("denied");
    }

    @Test
    void failsLoudlyWhenNoHandlerExists() {
        Mediator mediator = new Mediator(List.of(new PingHandler()), List.of());

        assertThatThrownBy(() -> mediator.send(new Unhandled()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No handler");
    }

    @Test
    void rejectsTwoHandlersForOneRequestType() {
        assertThatThrownBy(() -> new Mediator(List.of(new PingHandler(), new PingHandler()), List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Two handlers");
    }
}
