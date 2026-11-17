package com.schoolos.core.application.mediator;

/**
 * Handles exactly one request type.
 *
 * @param <Q> the request type
 * @param <R> the response type
 */
public interface RequestHandler<Q extends Request<R>, R> {

    Class<Q> requestType();

    R handle(Q request);
}
