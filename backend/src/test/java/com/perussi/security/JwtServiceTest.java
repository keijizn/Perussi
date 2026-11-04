package com.perussi.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.perussi.model.Usuario;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private static final String SECRET = "test-only-secret-with-at-least-32-bytes";
    private final JwtService service = new JwtService(SECRET);

    @Test
    void issuesTokenWithTwoHourExpiration() {
        Instant before = Instant.now();
        String token = service.generateToken(Usuario.builder()
                .email("cliente@example.com").role("CUSTOMER").build());
        assertEquals("cliente@example.com", service.validateToken(token));
        Instant expires = JWT.decode(token).getExpiresAtAsInstant();
        assertTrue(expires.isAfter(before.plusSeconds(7198)));
        assertTrue(expires.isBefore(Instant.now().plusSeconds(7201)));
    }

    @Test
    void rejectsMissingOrShortSecret() {
        assertThrows(IllegalArgumentException.class, () -> new JwtService(null));
        assertThrows(IllegalArgumentException.class, () -> new JwtService("short"));
        assertThrows(IllegalArgumentException.class, () -> new JwtService(" ".repeat(40)));
    }

    @Test
    void rejectsExpiredAndIncorrectlySignedTokens() {
        String expired = JWT.create().withIssuer("perussi-api")
                .withSubject("cliente@example.com").withExpiresAt(Instant.now().minusSeconds(60))
                .sign(Algorithm.HMAC256(SECRET));
        String forged = JWT.create().withIssuer("perussi-api")
                .withSubject("cliente@example.com").withExpiresAt(Instant.now().plusSeconds(60))
                .sign(Algorithm.HMAC256("different-signing-key"));
        assertThrows(JWTVerificationException.class, () -> service.validateToken(expired));
        assertThrows(JWTVerificationException.class, () -> service.validateToken(forged));
    }
}
