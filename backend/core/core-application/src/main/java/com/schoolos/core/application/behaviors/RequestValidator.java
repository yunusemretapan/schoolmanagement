package com.schoolos.core.application.behaviors;

import com.schoolos.core.application.mediator.Request;
import java.util.Map;

/**
 * Rules for one request type that annotations cannot express (e.g. "an e-mail or a Turkish mobile number").
 * Returns field → stable error code; empty means valid.
 *
 * @param <Q> the request type
 */
public interface RequestValidator<Q extends Request<?>> {

    Class<Q> requestType();

    Map<String, String> validate(Q request);
}
