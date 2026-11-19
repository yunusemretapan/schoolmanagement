package com.schoolos.application.features.identity.queries;

import com.schoolos.application.features.identity.dtos.CurrentUserResponse;
import com.schoolos.core.application.mediator.Request;
import com.schoolos.core.security.Permission;
import com.schoolos.core.security.RequiresPermission;

/** Who am I? Always the caller's own account, in the tenant of their token (spec 0003, Req. 10). */
@RequiresPermission(Permission.SELF_READ)
public record GetCurrentUserQuery() implements Request<CurrentUserResponse> {}
