package com.schoolos.application.features.identity.commands;

import com.schoolos.application.features.identity.dtos.StaffDetail;
import com.schoolos.core.application.mediator.Request;
import com.schoolos.core.security.Permission;
import com.schoolos.core.security.RequiresPermission;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Pasifleştir: the account stays, its access ends at once on every device (spec 0004, Req. 8). */
@RequiresPermission(Permission.STAFF_MANAGE)
public record DeactivateStaffCommand(@NotNull UUID staffId) implements Request<StaffDetail> {}
