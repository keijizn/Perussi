package com.perussi.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.perussi.model.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.nio.charset.StandardCharsets;

@Service
public class JwtService {

    private final Algorithm algorithm;

    public JwtService(@Value("${security.jwt.secret}") String secret) {
        if (secret == null || secret.isBlank()
                || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET deve conter pelo menos 32 bytes e ser aleatório");
        }
        this.algorithm = Algorithm.HMAC256(secret);
    }

    public String generateToken(Usuario usuario) {

        return JWT.create()
                .withIssuer("perussi-api")
                .withSubject(usuario.getEmail())
                .withClaim("uid", usuario.getId() == null ? null : usuario.getId().toString())
                .withClaim("session", usuario.getVersaoSessao() == null ? "0" : usuario.getVersaoSessao())
                .withClaim("role", usuario.getRole())
                .withExpiresAt(generateExpirationDate())
                .sign(algorithm);
    }

    public String validateToken(String token) {

        return JWT.require(algorithm)
                .withIssuer("perussi-api")
                .build()
                .verify(token)
                .getSubject();
    }

    private Instant generateExpirationDate() {
        return Instant.now().plus(2, ChronoUnit.HOURS);
    }
}
