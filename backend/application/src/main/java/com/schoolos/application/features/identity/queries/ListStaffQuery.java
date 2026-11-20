package com.schoolos.application.features.identity.queries;

import com.schoolos.application.features.identity.dtos.StaffPage;
import com.schoolos.core.application.mediator.Request;
import com.schoolos.core.security.Permission;
import com.schoolos.core.security.RequiresPermission;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** The institution's staff, 50 per page, optionally searched (spec 0004, Req. 3). {@code page} counts from 0. */
@RequiresPermission(Permission.STAFF_MANAGE)
public record ListStaffQuery(
        String search, @Min(0) @Max(10_000) int page) implements Request<StaffPage> {

    @Override
    public String toString() {
        return "ListStaffQuery[page=" + page + "]"; // the search may hold a name or a phone number
    }
}
