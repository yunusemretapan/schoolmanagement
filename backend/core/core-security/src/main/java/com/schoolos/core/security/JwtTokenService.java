package com.schoolos.core.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** HS256-signed JWT. Expiry is judged against the injected {@link Clock} (docs/testing.md, Determinism). */
public final class JwtTokenService implements TokenService {

    static final String TENANT = "tid";
    static final String SESSION = "sid";
    static final String ROLES = "roles";

    private final byte[] secret;
    private final Clock clock;

    public JwtTokenService(byte[] secret, Clock clock) {
        if (secret.length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes");
        }
        this.secret = secret.clone();
        this.clock = clock;
    }

    @Override
    public String issue(UUID userId, UUID tenantId, UUID sessionId, Set<String> roles) {
        Instant now = clock.instant();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(userId.toString())
                .claim(TENANT, tenantId.toString())
                .claim(SESSION, sessionId.toString())
                .claim(ROLES, List.copyOf(roles))
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(ACCESS_TOKEN_TTL)))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        try {
            jwt.sign(new MACSigner(secret));
        } catch (JOSEException e) {
            throw new IllegalStateException("Could not sign access token", e);
        }
        return jwt.serialize();
    }

    @Override
    public Optional<AccessClaims> verify(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm()) || !jwt.verify(new MACVerifier(secret))) {
                return Optional.empty();
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date expiry = claims.getExpirationTime();
            String subject = claims.getSubject();
            String tenant = claims.getStringClaim(TENANT);
            String session = claims.getStringClaim(SESSION);
            if (expiry == null || subject == null || tenant == null || session == null) {
                return Optional.empty();
            }
            if (!clock.instant().isBefore(expiry.toInstant())) {
                return Optional.empty();
            }
            List<String> roles = claims.getStringListClaim(ROLES);
            return Optional.of(new AccessClaims(
                    UUID.fromString(subject),
                    UUID.fromString(tenant),
                    UUID.fromString(session),
                    roles == null ? Set.of() : new HashSet<>(roles),
                    expiry.toInstant()));
        } catch (ParseException | JOSEException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
