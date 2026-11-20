package com.schoolos.application.features.identity.dtos;

import com.schoolos.application.features.identity.ports.StaffDirectory;
import com.schoolos.domain.identity.AccountStatus;
import com.schoolos.domain.identity.Role;
import java.util.List;
import java.util.UUID;

/** One row of the staff list (spec 0004, Req. 3). Roles include Veli, shown as a badge only (G3). */
public record StaffSummary(
        UUID id, String fullName, String email, String phone, List<String> roles, AccountStatus status) {

    public StaffSummary {
        roles = List.copyOf(roles);
    }

    public static StaffSummary of(StaffDirectory.Row row) {
        return new StaffSummary(
                row.user().id(),
                row.user().fullName(),
                row.identity().email(),
                row.identity().phone(),
                row.user().roles().stream().sorted().map(Role::name).toList(),
                row.identity().status());
    }
}
