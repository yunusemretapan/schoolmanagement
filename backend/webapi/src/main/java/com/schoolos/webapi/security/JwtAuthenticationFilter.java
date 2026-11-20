package com.schoolos.webapi.security;

import com.schoolos.application.features.identity.rules.RolePermissions;
import com.schoolos.core.security.AccessClaims;
import com.schoolos.core.security.CurrentUser;
import com.schoolos.core.security.CurrentUserHolder;
import com.schoolos.core.security.SessionValidator;
import com.schoolos.core.security.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Turns a valid bearer token into the request's {@link CurrentUser}. The tenant comes from the signed token only;
 * no header, parameter or body field can change it (BR-1, spec 0003 AC-7). A token whose session was ended is
 * ignored (AC-9). Anything invalid simply leaves the request anonymous: the security chain then answers 401.
 */
public final class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final TokenService tokens;
    private final SessionValidator sessions;

    public JwtAuthenticationFilter(TokenService tokens, SessionValidator sessions) {
        this.tokens = tokens;
        this.sessions = sessions;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            bearerToken(request)
                    .flatMap(tokens::verify)
                    .flatMap(claims -> sessions.currentRoles(claims.sessionId(), claims.tenantId())
                            .map(roles -> new AccessClaims(
                                    claims.userId(), claims.tenantId(), claims.sessionId(), roles, claims.expiresAt())))
                    .ifPresent(JwtAuthenticationFilter::signIn);
            chain.doFilter(request, response);
        } finally {
            CurrentUserHolder.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private static Optional<String> bearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER) || header.length() == BEARER.length()) {
            return Optional.empty();
        }
        return Optional.of(header.substring(BEARER.length()));
    }

    private static void signIn(AccessClaims claims) {
        CurrentUser user = new CurrentUser(
                claims.userId(),
                claims.tenantId(),
                claims.sessionId(),
                claims.roles(),
                RolePermissions.forRoleNames(claims.roles()));
        CurrentUserHolder.set(user);
        List<SimpleGrantedAuthority> authorities = claims.roles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, authorities));
        SecurityContextHolder.setContext(context);
    }
}
