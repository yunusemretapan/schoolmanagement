package com.schoolos.application.features.identity.queries;

import com.schoolos.application.features.identity.dtos.StaffDetail;
import com.schoolos.core.application.mediator.Request;
import com.schoolos.core.security.Permission;
import com.schoolos.core.security.RequiresPermission;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@RequiresPermission(Permission.STAFF_MANAGE)
public record GetStaffQuery(@NotNull UUID staffId) implements Request<StaffDetail> {}
