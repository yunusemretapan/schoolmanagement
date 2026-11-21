package com.schoolos.webapi.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.schoolos.core.security.CurrentUserHolder;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Spec 0003, AC-7 (the tenant comes from the signed token only) and AC-9 (a logged-out session's access token no
 * longer counts).
 */
class JwtAuthenticationFilterTest {

    private static final UUID TENANT_A = UUID.fromString("01900000-0000-7000-8000-0000000000aa");
    private static final UUID TENANT_B = UUID.fromString("01900000-0000-7000-8000-0000000000bb");

    private final SecurityTestContext web = new SecurityTestContext();
    private final UUID session = UUID.randomUUID();
    private final String tokenForA;

    JwtAuthenticationFilterTest() {
        SecurityTestContext.OPEN_SESSIONS.add(session);
        tokenForA = web.tokens().issue(UUID.randomUUID(), TENANT_A, session, Set.of("TEACHER"));
    }

    @AfterEach
    void close() {
        web.close();
    }

    @Test
    void requestRunsInTheTokensTenant() throws Exception {
        web.mvc
                .perform(get("/api/v1/me").header("Authorization", "Bearer " + tokenForA))
                .andExpect(content().string(TENANT_A.toString()));
    }

    @Test
    void tenantHintsInHeadersOrParametersAreIgnored() throws Exception {
        web.mvc
                .perform(get("/api/v1/me")
                        .header("Authorization", "Bearer " + tokenForA)
                        .header("X-Tenant-Id", TENANT_B.toString())
                        .param("tenantId", TENANT_B.toString()))
                .andExpect(content().string(TENANT_A.toString()));
    }

    @Test
    void endedSessionsTokenNoLongerGetsIn() throws Exception {
        SecurityTestContext.OPEN_SESSIONS.remove(session);

        web.mvc
                .perform(get("/api/v1/me").header("Authorization", "Bearer " + tokenForA))
                .andExpect(status().isUnauthorized());
    }

    /** Spec 0004, Req. 7 / AC-7 (plan 0004, R2): the token says TEACHER, the account is ADMIN now. */
    @Test
    void permissionsComeFromTheAccountsCurrentRolesNotFromTheToken() throws Exception {
        SecurityTestContext.CURRENT_ROLES.put(session, Set.of("ADMIN"));

        web.mvc
                .perform(get("/api/v1/test/permissions").header("Authorization", "Bearer " + tokenForA))
                .andExpect(
                        content().string("[ACADEMIC_STRUCTURE_MANAGE, SELF_READ, STAFF_MANAGE]")); // spec 0005 adds one

        SecurityTestContext.CURRENT_ROLES.put(session, Set.of("TEACHER"));

        web.mvc
                .perform(get("/api/v1/test/permissions").header("Authorization", "Bearer " + tokenForA))
                .andExpect(content().string("[SELF_READ]"));
    }

    @Test
    void currentUserIsClearedAfterTheRequest() throws Exception {
        web.mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + tokenForA));

        assertThat(CurrentUserHolder.current()).isEmpty();
    }
}
