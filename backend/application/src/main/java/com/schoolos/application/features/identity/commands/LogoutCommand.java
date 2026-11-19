package com.schoolos.application.features.identity.commands;

import com.schoolos.core.application.mediator.Request;
import com.schoolos.core.security.Permission;
import com.schoolos.core.security.RequiresPermission;

/** End the caller's session on this device only (spec 0003, Req. 7). The session comes from the access token. */
@RequiresPermission(Permission.SESSION_END)
public record LogoutCommand() implements Request<Void> {}
