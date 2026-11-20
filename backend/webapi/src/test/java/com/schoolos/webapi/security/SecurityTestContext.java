package com.schoolos.webapi.security;

import com.schoolos.core.security.CurrentUser;
import com.schoolos.core.security.CurrentUserHolder;
import com.schoolos.core.security.SessionValidator;
import com.schoolos.core.security.TokenService;
import jakarta.servlet.Filter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * The real {@link SecurityConfig} filter chain in a plain, in-memory Spring web context: no Spring Boot startup, no
 * database, no network (ADR 0003: unit level). Endpoints are stand-ins that echo the current user's tenant.
 */
final class SecurityTestContext implements AutoCloseable {

    static final String SECRET =
            Base64.getEncoder().encodeToString("security-test-secret-security-test!!".getBytes(StandardCharsets.UTF_8));
    static final String ALLOWED_ORIGIN = "http://localhost:8081";

    /** Sessions the stand-in validator treats as open; tests add and remove ids. */
    static final Set<UUID> OPEN_SESSIONS = new HashSet<>();

    /** The account's current roles per session, as the database would answer; TEACHER when not set. */
    static final Map<UUID, Set<String>> CURRENT_ROLES = new HashMap<>();

    final AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
    final MockMvc mvc;

    SecurityTestContext() {
        OPEN_SESSIONS.clear();
        CURRENT_ROLES.clear();
        context.setServletContext(new MockServletContext());
        context.getEnvironment()
                .getPropertySources()
                .addFirst(new MapPropertySource(
                        "test", Map.of("app.security.jwt-secret", SECRET, "app.cors.allowed-origins", ALLOWED_ORIGIN)));
        context.register(Web.class, SecurityConfig.class, Endpoints.class);
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
                .build();
    }

    TokenService tokens() {
        return context.getBean(TokenService.class);
    }

    @Override
    public void close() {
        context.close();
    }

    @Configuration
    @EnableWebMvc
    static class Web {

        @Bean
        SessionValidator sessionValidator() {
            return (sessionId, tenantId) -> OPEN_SESSIONS.contains(sessionId)
                    ? Optional.of(CURRENT_ROLES.getOrDefault(sessionId, Set.of("TEACHER")))
                    : Optional.empty();
        }
    }

    @RestController
    static class Endpoints {

        @GetMapping(SecurityConfig.STATUS_PATH)
        String status() {
            return "up";
        }

        @PostMapping(SecurityConfig.LOGIN_PATH)
        String login() {
            return "login";
        }

        @PostMapping(SecurityConfig.LOGOUT_PATH)
        String logout() {
            return "logout";
        }

        /** Echoes the tenant the request runs in. */
        @GetMapping("/api/v1/me")
        String me() {
            return CurrentUserHolder.current()
                    .map(CurrentUser::tenantId)
                    .map(UUID::toString)
                    .orElse("none");
        }

        /** Echoes the permissions the request runs with. */
        @GetMapping("/api/v1/test/permissions")
        String permissions() {
            return CurrentUserHolder.current()
                    .map(user -> user.permissions().stream()
                            .map(Enum::name)
                            .sorted()
                            .toList()
                            .toString())
                    .orElse("none");
        }
    }
}
