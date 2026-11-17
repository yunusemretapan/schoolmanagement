package com.schoolos.core.application.mediator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Dispatches a request to its single handler through the pipeline behaviors, in order. */
public final class Mediator {

    private final Map<Class<?>, RequestHandler<?, ?>> handlers = new HashMap<>();
    private final List<PipelineBehavior> behaviors;

    public Mediator(List<? extends RequestHandler<?, ?>> handlers, List<PipelineBehavior> behaviors) {
        for (RequestHandler<?, ?> handler : handlers) {
            RequestHandler<?, ?> previous = this.handlers.put(handler.requestType(), handler);
            if (previous != null) {
                throw new IllegalStateException(
                        "Two handlers for " + handler.requestType().getName());
            }
        }
        this.behaviors = List.copyOf(behaviors);
    }

    public <R> R send(Request<R> request) {
        RequestHandler<Request<R>, R> handler = handlerFor(request);
        Supplier<R> chain = () -> handler.handle(request);
        for (int i = behaviors.size() - 1; i >= 0; i--) {
            PipelineBehavior behavior = behaviors.get(i);
            Supplier<R> next = chain;
            chain = () -> behavior.handle(request, next);
        }
        return chain.get();
    }

    @SuppressWarnings("unchecked") // the map is keyed by the handler's own request type
    private <R> RequestHandler<Request<R>, R> handlerFor(Request<R> request) {
        RequestHandler<?, ?> handler = handlers.get(request.getClass());
        if (handler == null) {
            throw new IllegalStateException(
                    "No handler for " + request.getClass().getName());
        }
        return (RequestHandler<Request<R>, R>) handler;
    }
}
