package com.schoolos.application.features.identity.commands;

import com.schoolos.application.features.identity.dtos.StaffDetail;
import com.schoolos.core.application.mediator.Request;
import com.schoolos.core.security.Permission;
import com.schoolos.core.security.RequiresPermission;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Back from inactive: active with the old password, or invited again (spec 0004, Req. 8, G5). */
@RequiresPermission(Permission.STAFF_MANAGE)
public record ReactivateStaffCommand(@NotNull UUID staffId) implements Request<StaffDetail> {}
