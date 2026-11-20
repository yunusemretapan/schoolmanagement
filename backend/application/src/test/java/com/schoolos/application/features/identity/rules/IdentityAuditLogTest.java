package com.schoolos.application.features.identity.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.schoolos.core.application.audit.AuditEntry;
import com.schoolos.domain.identity.AuditAction;
import com.schoolos.domain.identity.AuditEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Spec 0005, plan R1: staff-management records land in the shared log unchanged, the person named by id only. */
class IdentityAuditLogTest {

    @Test
    void aStaffRecordBecomesAUserTargetedEntryWithTheSameWhoWhatAndWhen() {
        List<AuditEntry> shared = new ArrayList<>();
        AuditEvent event = new AuditEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                AuditAction.ROLE_ADDED,
                "TEACHER",
                Instant.parse("2026-11-21T09:00:00Z"));

        new IdentityAuditLog(shared::add).append(event);

        assertThat(shared).singleElement().satisfies(entry -> {
            assertThat(entry.id()).isEqualTo(event.id());
            assertThat(entry.tenantId()).isEqualTo(event.tenantId());
            assertThat(entry.actorUserId()).isEqualTo(event.actorUserId());
            assertThat(entry.targetType()).isEqualTo(AuditEntry.USER);
            assertThat(entry.targetId()).isEqualTo(event.targetUserId());
            assertThat(entry.targetName()).isNull();
            assertThat(entry.action()).isEqualTo("ROLE_ADDED");
            assertThat(entry.detail()).isEqualTo("TEACHER");
            assertThat(entry.at()).isEqualTo(event.at());
        });
    }
}
