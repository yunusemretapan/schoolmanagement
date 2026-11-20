package com.schoolos.webapi.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Spec 0003, AC-6 at the HTTP door: only the status query and login/refresh are open. */
class SecurityConfigTest {

    private final SecurityTestContext web = new SecurityTestContext();

    @AfterEach
    void close() {
        web.close();
    }

    @Test
    void statusQueryStaysOpen() throws Exception {
        web.mvc
                .perform(get("/api/v1/system/status"))
                .andExpect(status().isOk())
                .andExpect(content().string("up"));
    }

    @Test
    void loginIsOpen() throws Exception {
        web.mvc.perform(post("/api/v1/auth/login")).andExpect(status().isOk());
    }

    @Test
    void logoutIsOpenSoAnExpiredAccessTokenCanStillSignOutReview1H1() throws Exception {
        web.mvc.perform(post("/api/v1/auth/logout")).andExpect(status().isOk());
    }

    @Test
    void everythingElseWithoutATokenIs401WithAProblemCode() throws Exception {
        web.mvc
                .perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHENTICATED"));
        web.mvc.perform(get("/api/v1/anything-not-mapped")).andExpect(status().isUnauthorized());
    }

    @Test
    void aValidTokenOfAnOpenSessionGetsIn() throws Exception {
        UUID session = UUID.randomUUID();
        SecurityTestContext.OPEN_SESSIONS.add(session);
        String token = web.tokens().issue(UUID.randomUUID(), UUID.randomUUID(), session, Set.of("ADMIN"));

        web.mvc
                .perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void tamperedOrMalformedTokensAre401() throws Exception {
        UUID session = UUID.randomUUID();
        SecurityTestContext.OPEN_SESSIONS.add(session);
        String token = web.tokens().issue(UUID.randomUUID(), UUID.randomUUID(), session, Set.of("ADMIN"));

        web.mvc
                .perform(get("/api/v1/me").header("Authorization", "Bearer " + token + "x"))
                .andExpect(status().isUnauthorized());
        web.mvc.perform(get("/api/v1/me").header("Authorization", "Bearer ")).andExpect(status().isUnauthorized());
        web.mvc
                .perform(get("/api/v1/me").header("Authorization", "Basic " + token))
                .andExpect(status().isUnauthorized());
    }

    /** Spec 0004, Req. 5, 6: the invitation link's two calls are open; the link is the credential. */
    @Test
    void invitationLookupAndAcceptAreOpen() throws Exception {
        web.mvc.perform(post("/api/v1/invitations/lookup")).andExpect(status().isOk());
        web.mvc.perform(post("/api/v1/invitations/accept")).andExpect(status().isOk());
    }

    /** Spec 0004, AC-1: staff management stays behind the door, and only POST invitation calls are open. */
    @Test
    void staffManagementAndOtherInvitationCallsNeedASession() throws Exception {
        web.mvc.perform(get("/api/v1/staff")).andExpect(status().isUnauthorized());
        web.mvc.perform(post("/api/v1/staff")).andExpect(status().isUnauthorized());
        web.mvc.perform(get("/api/v1/invitations/lookup")).andExpect(status().isUnauthorized());
        web.mvc.perform(post("/api/v1/invitations/other")).andExpect(status().isUnauthorized());
    }
}
