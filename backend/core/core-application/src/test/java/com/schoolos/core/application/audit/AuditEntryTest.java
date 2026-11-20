package com.schoolos.core.application.audit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Spec 0005, Req. 10, plan R1: an audit entry always says who, which record, what and when. */
class AuditEntryTest {

    private static final UUID ID = UUID.randomUUID();
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID TARGET = UUID.randomUUID();
    private static final Instant AT = Instant.parse("2026-11-21T09:00:00Z");

    @Test
    void aRecordOfTheAcademicStructureCarriesItsName() {
        assertThatCode(() -> new AuditEntry(ID, TENANT, ACTOR, "TERM", TARGET, "2026–2027", "TERM_CREATED", null, AT))
                .doesNotThrowAnyException();
    }

    @Test
    void aPersonIsNamedByIdOnly() {
        assertThatThrownBy(() -> new AuditEntry(
                        ID, TENANT, ACTOR, AuditEntry.USER, TARGET, "Ali Yılmaz", "ROLE_ADDED", "TEACHER", AT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void whoWhichRecordWhatAndWhenAreRequired() {
        assertThatThrownBy(() -> new AuditEntry(ID, TENANT, null, "TERM", TARGET, null, "TERM_CREATED", null, AT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuditEntry(ID, TENANT, ACTOR, " ", TARGET, null, "TERM_CREATED", null, AT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuditEntry(ID, TENANT, ACTOR, "TERM", null, null, "TERM_CREATED", null, AT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuditEntry(ID, TENANT, ACTOR, "TERM", TARGET, null, "", null, AT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuditEntry(ID, TENANT, ACTOR, "TERM", TARGET, null, "TERM_CREATED", null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
