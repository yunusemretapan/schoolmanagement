package com.schoolos.application.features.system.queries;

import com.schoolos.application.features.system.dtos.SystemStatusResponse;
import com.schoolos.core.application.mediator.Request;
import com.schoolos.core.security.Public;

/**
 * Is the system up, and which version is running? Public by design (spec 0001, AC-1): the one exception to default
 * deny that carries no credentials (spec 0003, Req. 9).
 */
@Public
public record GetSystemStatusQuery() implements Request<SystemStatusResponse> {}
