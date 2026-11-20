package com.schoolos.application.features.identity.ports;

import com.schoolos.application.features.identity.rules.StaffSearchTerm;
import com.schoolos.domain.identity.LoginIdentity;
import com.schoolos.domain.identity.UserAccount;
import java.util.List;
import java.util.Optional;

/** Reads over the bound tenant's staff (spec 0004, Req. 3, 9): accounts holding at least one staff role. */
public interface StaffDirectory {

    record Row(UserAccount user, LoginIdentity identity) {}

    record Page(List<Row> rows, long total) {
        public Page {
            rows = List.copyOf(rows);
        }
    }

    /** Ordered by name, then id; {@code page} counts from 0. */
    Page search(Optional<StaffSearchTerm> term, int page, int pageSize);

    long countActiveAdmins();
}
