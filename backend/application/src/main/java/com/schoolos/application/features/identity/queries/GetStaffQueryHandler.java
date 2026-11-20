package com.schoolos.application.features.identity.queries;

import com.schoolos.application.features.identity.commands.StaffAccounts;
import com.schoolos.application.features.identity.dtos.StaffDetail;
import com.schoolos.core.application.mediator.RequestHandler;

/** Spec 0004, AC-2: another institution's staff member is not found, exactly like an unknown id. */
public final class GetStaffQueryHandler implements RequestHandler<GetStaffQuery, StaffDetail> {

    private final StaffAccounts staff;

    public GetStaffQueryHandler(StaffAccounts staff) {
        this.staff = staff;
    }

    @Override
    public Class<GetStaffQuery> requestType() {
        return GetStaffQuery.class;
    }

    @Override
    public StaffDetail handle(GetStaffQuery query) {
        return staff.detail(staff.load(query.staffId()), StaffAccounts.caller());
    }
}
