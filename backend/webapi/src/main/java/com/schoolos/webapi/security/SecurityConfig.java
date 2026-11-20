package com.schoolos.webapi.security;

import com.schoolos.application.features.identity.ports.SecretGenerator;
import com.schoolos.application.features.identity.ports.SourceRateLimiter;
import com.schoolos.core.crosscutting.ids.IdGenerator;
import com.schoolos.core.crosscutting.ids.UuidV7Generator;
import com.schoolos.core.security.Argon2PasswordHasher;
import com.schoolos.core.security.AuthorizationException;
import com.schoolos.core.security.JwtTokenService;
import com.schoolos.core.security.PasswordHasher;
import com.schoolos.core.security.SessionValidator;
import com.schoolos.core.security.TokenService;
import com.schoolos.core.security.UnauthenticatedException;
import com.schoolos.webapi.errors.Problems;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.Base64;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * HTTP security: stateless bearer tokens, default deny at the door (spec 0003, Req. 9). Only the status query and
 * login/refresh/logout are open (logout must work after the access token expired: review 1, H-1); everything else needs a valid access token, and the Mediator's AuthorizationBehavior
 * checks the permission behind it. CSRF protection is off because no endpoint trusts an ambient credential except
 * refresh, whose cookie is SameSite=Strict and scoped to the auth path ({@link RefreshCookie}).
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

    public static final String STATUS_PATH = "/api/v1/system/status";
    public static final String LOGIN_PATH = "/api/v1/auth/login";
    public static final String REFRESH_PATH = "/api/v1/auth/refresh";
    public static final String LOGOUT_PATH = "/api/v1/auth/logout";
    /** The invitation link's open door (spec 0004, Req. 5, 6): the link itself is the credential. */
    public static final String INVITATION_LOOKUP_PATH = "/api/v1/invitations/lookup";

    public static final String INVITATION_ACCEPT_PATH = "/api/v1/invitations/accept";

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, TokenService tokens, SessionValidator sessions)
            throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults()) // the bean named corsConfigurationSource below
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .requestCache(cache -> cache.disable())
                .anonymous(anonymous -> anonymous.disable())
                .authorizeHttpRequests(requests -> requests.requestMatchers(HttpMethod.GET, STATUS_PATH)
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                LOGIN_PATH,
                                REFRESH_PATH,
                                LOGOUT_PATH,
                                INVITATION_LOOKUP_PATH,
                                INVITATION_ACCEPT_PATH)
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, e) -> write(
                                response,
                                HttpStatus.UNAUTHORIZED,
                                Problems.json(HttpStatus.UNAUTHORIZED, UnauthenticatedException.CODE)))
                        .accessDeniedHandler((request, response, e) -> write(
                                response,
                                HttpStatus.FORBIDDEN,
                                Problems.json(HttpStatus.FORBIDDEN, AuthorizationException.CODE))))
                .addFilterBefore(
                        new JwtAuthenticationFilter(tokens, sessions), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static void write(HttpServletResponse response, HttpStatus status, String body) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write(body);
    }

    /** Cross-origin browser calls only from configured origins; no credentials (Expo web keeps tokens in memory). */
    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(properties.allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    /** Passwords are checked by LoginCommandHandler; this keeps Spring Boot from creating a default user. */
    @Bean
    AuthenticationManager noAuthenticationManager() {
        return authentication -> {
            throw new ProviderNotFoundException("Authentication happens in the identity feature");
        };
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    IdGenerator idGenerator(Clock clock) {
        return new UuidV7Generator(clock);
    }

    @Bean
    PasswordHasher passwordHasher() {
        return new Argon2PasswordHasher();
    }

    /** {@code app.security.jwt-secret}: base64, at least 32 bytes; required outside the dev profile. */
    @Bean
    TokenService tokenService(@Value("${app.security.jwt-secret}") String secret, Clock clock) {
        return new JwtTokenService(Base64.getDecoder().decode(secret.strip()), clock);
    }

    @Bean
    SecretGenerator secretGenerator() {
        return SecretGenerator.secureRandom();
    }

    @Bean
    SourceRateLimiter sourceRateLimiter(Clock clock) {
        return new InMemorySourceRateLimiter(clock);
    }
}
