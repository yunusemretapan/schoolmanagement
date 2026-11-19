package com.schoolos.webapi.controllers;

import com.schoolos.application.features.identity.commands.LoginCommand;
import com.schoolos.application.features.identity.commands.LogoutCommand;
import com.schoolos.application.features.identity.commands.RefreshSessionCommand;
import com.schoolos.application.features.identity.dtos.AuthOutcome;
import com.schoolos.core.application.mediator.Mediator;
import com.schoolos.domain.identity.ClientType;
import com.schoolos.webapi.errors.Problems;
import com.schoolos.webapi.security.RefreshCookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sign-in, session refresh and sign-out (spec 0003). The web app's refresh token travels only in an HttpOnly cookie;
 * the mobile app receives it in the body and keeps it in SecureStore (plan 0003, R2).
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    public record LoginRequest(String identifier, String password, ClientType clientType) {

        @Override
        public String toString() {
            return "LoginRequest[clientType=" + clientType + "]";
        }
    }

    public record RefreshRequest(String refreshToken) {

        @Override
        public String toString() {
            return "RefreshRequest[]";
        }
    }

    /** Web: the refresh token is in the cookie, not here. */
    public record WebTokenResponse(String accessToken, long expiresInSeconds) {}

    public record MobileTokenResponse(String accessToken, long expiresInSeconds, String refreshToken) {}

    private final Mediator mediator;

    public AuthController(Mediator mediator) {
        this.mediator = mediator;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest body, HttpServletRequest request) {
        AuthOutcome outcome = mediator.send(
                new LoginCommand(body.identifier(), body.password(), body.clientType(), request.getRemoteAddr()));
        return respond(outcome);
    }

    /** The mobile app sends the token in the body; the web app's browser sends the cookie. */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @RequestBody(required = false) RefreshRequest body,
            @CookieValue(name = RefreshCookie.NAME, required = false) String cookie) {
        String token = body != null && body.refreshToken() != null ? body.refreshToken() : cookie;
        AuthOutcome outcome = mediator.send(new RefreshSessionCommand(token));
        if (outcome instanceof AuthOutcome.Refused && cookie != null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, RefreshCookie.clear().toString())
                    .body(Problems.of(HttpStatus.UNAUTHORIZED, ((AuthOutcome.Refused) outcome).code()));
        }
        return respond(outcome);
    }

    /**
     * Works with an expired access token too (review 1, H-1): the session is then found from the refresh token in the
     * body (mobile) or the cookie (web). The cookie is cleared whatever happens.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestBody(required = false) RefreshRequest body,
            @CookieValue(name = RefreshCookie.NAME, required = false) String cookie) {
        String token = body != null && body.refreshToken() != null ? body.refreshToken() : cookie;
        mediator.send(new LogoutCommand(token));
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, RefreshCookie.clear().toString())
                .build();
    }

    private static ResponseEntity<?> respond(AuthOutcome outcome) {
        return switch (outcome) {
            case AuthOutcome.Issued issued
            when issued.clientType() == ClientType.WEB ->
                ResponseEntity.ok()
                        .header(
                                HttpHeaders.SET_COOKIE,
                                RefreshCookie.issue(issued.refreshToken()).toString())
                        .body(new WebTokenResponse(issued.accessToken(), issued.accessExpiresInSeconds()));
            case AuthOutcome.Issued issued ->
                ResponseEntity.ok()
                        .body(new MobileTokenResponse(
                                issued.accessToken(), issued.accessExpiresInSeconds(), issued.refreshToken()));
            case AuthOutcome.Refused refused ->
                ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Problems.of(HttpStatus.UNAUTHORIZED, refused.code()));
        };
    }
}
