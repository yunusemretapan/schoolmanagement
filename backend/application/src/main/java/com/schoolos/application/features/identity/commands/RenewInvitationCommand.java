package com.schoolos.application.features.identity.commands;

import com.schoolos.application.features.identity.dtos.InvitationLink;
import com.schoolos.core.application.mediator.Request;
import com.schoolos.core.security.Permission;
import com.schoolos.core.security.RequiresPermission;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** A new link for an invited account; the previous one stops working (spec 0004, Req. 5). */
@RequiresPermission(Permission.STAFF_MANAGE)
public record RenewInvitationCommand(@NotNull UUID staffId) implements Request<InvitationLink> {}
