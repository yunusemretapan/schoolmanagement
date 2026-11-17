package com.schoolos.application.features.system.queries;

import com.schoolos.application.features.system.dtos.SystemStatusResponse;
import com.schoolos.core.application.mediator.Request;

/** Is the system up, and which version is running? Public by design (spec 0001, AC-1). */
public record GetSystemStatusQuery() implements Request<SystemStatusResponse> {}
