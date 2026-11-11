package com.bidco.api_rest;

import com.bidco.api_rest.config.JwtUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiRestApplicationTests {

    private static final String VALID_SECRET = "test-only-secret-key-with-at-least-32-bytes";

    @Test
    void jwtRoundTripPreservesSubjectAndRole() {
        JwtUtil jwtUtil = new JwtUtil(VALID_SECRET, 3600000L);
        String token = jwtUtil.generateToken("42", "USER");

        assertEquals("42", jwtUtil.getClaims(token).getSubject());
        assertEquals("USER", jwtUtil.getClaims(token).get("role", String.class));
    }

    @Test
    void jwtSecretMustHaveAtLeastThirtyTwoBytes() {
        assertThrows(IllegalStateException.class, () -> new JwtUtil("too-short", 3600000L));
    }

    @Test
    void jwtRejectsTamperedTokens() {
        JwtUtil jwtUtil = new JwtUtil(VALID_SECRET, 3600000L);
        String token = jwtUtil.generateToken("42", "USER");

        assertThrows(Exception.class, () -> jwtUtil.getClaims(token + "tampered"));
    }

    @Test
    void jwtRejectsExpiredTokens() throws InterruptedException {
        JwtUtil jwtUtil = new JwtUtil(VALID_SECRET, 1L);
        String token = jwtUtil.generateToken("42", "USER");
        Thread.sleep(20);

        assertThrows(Exception.class, () -> jwtUtil.getClaims(token));
    }

    @Test
    void jwtExpirationMustBePositive() {
        assertThrows(IllegalStateException.class, () -> new JwtUtil(VALID_SECRET, 0L));
    }

    @Test
    void jwtDeUsuarioLlevaLaVersionDeSesionYElDeTrabajadorNo() {
        JwtUtil jwtUtil = new JwtUtil(VALID_SECRET, 3600000L);

        assertEquals(5, jwtUtil.getClaims(jwtUtil.generateToken("42", "USER", 5))
                .get(JwtUtil.CLAIM_VERSION, Integer.class));
        assertNull(jwtUtil.getClaims(jwtUtil.generateToken("42", "EXPERT"))
                .get(JwtUtil.CLAIM_VERSION, Integer.class));
    }
}
