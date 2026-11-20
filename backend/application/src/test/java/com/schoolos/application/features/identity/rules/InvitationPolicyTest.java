package com.schoolos.application.features.identity.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.schoolos.domain.identity.Invitation;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Spec 0004, Req. 5 / AC-5: 72 hours, once, the current secret only. */
class InvitationPolicyTest {

    private static final Instant NOW = Instant.parse("2026-11-20T09:00:00Z");

    private final Invitation invitation =
            InvitationPolicy.issue(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "secret-1", NOW);

    @Test
    void validFor71Hours59MinutesNotAt72Hours() {
        assertThat(InvitationPolicy.accepts(
                        invitation, "secret-1", NOW.plus(Duration.ofHours(71).plusMinutes(59))))
                .isTrue();
        assertThat(InvitationPolicy.accepts(invitation, "secret-1", NOW.plus(Duration.ofHours(72))))
                .isFalse();
    }

    @Test
    void usableOnceAndNeverAfterRevocation() {
        assertThat(InvitationPolicy.accepts(invitation.used(NOW), "secret-1", NOW))
                .isFalse();
        assertThat(InvitationPolicy.accepts(invitation.revoked(NOW), "secret-1", NOW))
                .isFalse();
    }

    @Test
    void aWrongSecretIsRefusedAndOnlyItsHashIsStored() {
        assertThat(InvitationPolicy.accepts(invitation, "secret-2", NOW)).isFalse();
        assertThat(invitation.tokenHash()).isNotEqualTo("secret-1").hasSize(64);
    }

    @Test
    void theTokenNamesTheInvitationAndParsesBack() {
        String token = InvitationPolicy.token(invitation, "secret-1");

        InvitationPolicy.Parsed parsed = InvitationPolicy.parse(token).orElseThrow();
        assertThat(parsed.invitationId()).isEqualTo(invitation.id());
        assertThat(parsed.secret()).isEqualTo("secret-1");
        assertThat(InvitationPolicy.parse("garbage")).isEmpty();
        assertThat(InvitationPolicy.parse(null)).isEmpty();
    }
}
