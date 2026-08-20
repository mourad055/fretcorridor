package com.fretcorridor.gateway.infrastructure.security;

import com.fretcorridor.gateway.domain.Actor;
import com.fretcorridor.gateway.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Émission et vérification des JWT. Le rôle et le tenant sont portés par le token
 * (claims "role", "tenantId") — c'est depuis ce token, jamais depuis un en-tête
 * modifiable, que le RBAC est évalué (RG-002).
 */
@Component
public class JwtService {

    private final SecretKey signingKey;
    private final Duration validity;

    public JwtService(
            @Value("${fretcorridor.jwt.secret}") String secret,
            @Value("${fretcorridor.jwt.validity-minutes:60}") long validityMinutes
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        this.validity = Duration.ofMinutes(validityMinutes);
    }

    public String issue(Actor actor) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(actor.actorId())
                .claim("phone", actor.phone())
                .claim("role", actor.role().name())
                .claim("tenantId", actor.tenantId());
        // Retransmis tel quel, jamais régénéré : seul service-ida sait ce qui doit
        // s'y trouver pour que service-exe/service-not/service-mkt le valident.
        if (actor.delegationToken() != null) {
            builder.claim("idaToken", actor.delegationToken());
        }
        return builder
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(validity)))
                .signWith(signingKey)
                .compact();
    }

    /** Retourne les claims si le token est valide et non expiré, sinon empty (jamais d'exception qui fuite). */
    public Optional<Claims> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static Role roleOf(Claims claims) {
        return Role.valueOf(claims.get("role", String.class));
    }

    /** Token service-ida retransmis, ou null si l'acteur ne s'est pas authentifié via service-ida. */
    public static String delegationTokenOf(Claims claims) {
        return claims.get("idaToken", String.class);
    }
}
