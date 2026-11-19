package com.schoolos.webapi.security;

import com.schoolos.application.features.identity.rules.SessionPolicy;
import java.time.Duration;
import org.springframework.http.ResponseCookie;

/**
 * The web app's refresh token lives only in this cookie (plan 0003, R2): HttpOnly (scripts cannot read it), Secure,
 * SameSite=Strict (no cross-site sends), and scoped to the auth endpoints. Its lifetime follows the web idle limit.
 */
public final class RefreshCookie {

    public static final String NAME = "sos_refresh";
    public static final String PATH = "/api/v1/auth";

    private RefreshCookie() {}

    public static ResponseCookie issue(String refreshToken) {
        return base(refreshToken).maxAge(SessionPolicy.WEB_IDLE_LIMIT).build();
    }

    public static ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private static ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(PATH);
    }
}
