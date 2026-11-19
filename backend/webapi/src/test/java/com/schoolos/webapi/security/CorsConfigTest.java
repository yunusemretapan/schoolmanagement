package com.schoolos.webapi.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Spec 0003, AC-12: the configured Expo web origin works; any other browser origin is refused. */
class CorsConfigTest {

    private final SecurityTestContext web = new SecurityTestContext();

    @AfterEach
    void close() {
        web.close();
    }

    @Test
    void configuredOriginPassesPreflightAndRequests() throws Exception {
        web.mvc
                .perform(options("/api/v1/auth/login")
                        .header("Origin", SecurityTestContext.ALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", SecurityTestContext.ALLOWED_ORIGIN));

        web.mvc
                .perform(post("/api/v1/auth/login").header("Origin", SecurityTestContext.ALLOWED_ORIGIN))
                .andExpect(status().isOk());

        UUID session = UUID.randomUUID();
        SecurityTestContext.OPEN_SESSIONS.add(session);
        String token = web.tokens().issue(UUID.randomUUID(), UUID.randomUUID(), session, Set.of("GUARDIAN"));
        web.mvc
                .perform(get("/api/v1/me")
                        .header("Origin", SecurityTestContext.ALLOWED_ORIGIN)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void otherOriginsAreRefused() throws Exception {
        web.mvc
                .perform(options("/api/v1/auth/login")
                        .header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());

        web.mvc
                .perform(post("/api/v1/auth/login").header("Origin", "http://evil.example"))
                .andExpect(status().isForbidden());
    }
}
