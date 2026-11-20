package com.schoolos.application.features.identity.queries;

import com.schoolos.application.features.identity.dtos.StaffPage;
import com.schoolos.application.features.identity.dtos.StaffSummary;
import com.schoolos.application.features.identity.ports.StaffDirectory;
import com.schoolos.application.features.identity.rules.StaffSearchTerm;
import com.schoolos.core.application.mediator.RequestHandler;

/** Spec 0004, Req. 3 / AC-3: only the bound tenant's staff, ordered by name. */
public final class ListStaffQueryHandler implements RequestHandler<ListStaffQuery, StaffPage> {

    public static final int PAGE_SIZE = 50;

    private final StaffDirectory directory;

    public ListStaffQueryHandler(StaffDirectory directory) {
        this.directory = directory;
    }

    @Override
    public Class<ListStaffQuery> requestType() {
        return ListStaffQuery.class;
    }

    @Override
    public StaffPage handle(ListStaffQuery query) {
        StaffDirectory.Page found = directory.search(StaffSearchTerm.parse(query.search()), query.page(), PAGE_SIZE);
        return new StaffPage(
                found.rows().stream().map(StaffSummary::of).toList(), query.page(), PAGE_SIZE, found.total());
    }
}
