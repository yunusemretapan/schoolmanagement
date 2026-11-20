package com.schoolos.application.features.identity.dtos;

import java.util.List;

/** A page of the staff list; {@code page} counts from 0 (spec 0004, Req. 3). */
public record StaffPage(List<StaffSummary> items, int page, int pageSize, long total) {

    public StaffPage {
        items = List.copyOf(items);
    }
}
