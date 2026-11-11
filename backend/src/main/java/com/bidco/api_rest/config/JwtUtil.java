package com.bidco.api_rest.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    public static final String CLAIM_VERSION = "tv";
    private static final String ISSUER = "bidco-api";
    private final long expirationMillis;
    private final SecretKey signingKey;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration:86400000}") long expirationMillis) {
        if (expirationMillis <= 0) {
            throw new IllegalStateException("jwt.expiration debe ser mayor que 0");
        }
        this.expirationMillis = expirationMillis;
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET debe configurarse antes de iniciar la aplicación");
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe contener al menos 32 bytes para HS256");
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /** Token sin versión de sesión (trabajadores y expertos, que no se revocan). */
    public String generateToken(String subject, String role) {
        return builder(subject, role).compact();
    }

    /** Token de usuario con la versión de sesión con la que se emite. */
    public String generateToken(String subject, String role, int tokenVersion) {
        return builder(subject, role).claim(CLAIM_VERSION, tokenVersion).compact();
    }

    private JwtBuilder builder(String subject, String role) {
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(subject)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(signingKey);
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getSubject(String token) {
        return getClaims(token).getSubject();
    }
}
