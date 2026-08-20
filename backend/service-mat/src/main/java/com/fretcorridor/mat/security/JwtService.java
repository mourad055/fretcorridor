package com.fretcorridor.mat.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.List;
import java.util.UUID;

// service-mat ne delivre aucun token - il valide ceux emis par service-ida,
// avec le meme secret partage. Aucun endpoint HTTP de service-mat n'est
// destine a etre appele par un humain aujourd'hui (CoutController est
// consomme en synchrone interne par OPT, jamais avec de JWT) - ce service
// est ajoute par coherence de dependances entre les 4 modules du perimetre
// Moteur, pas parce qu'un endpoint l'exige actuellement.
@Service
public class JwtService {

    @Value("${fretcorridor.jwt.secret}")
    private String secret;

    private SecretKey cle() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public Claims extraireClaims(String token) {
        return Jwts.parser().verifyWith(cle()).build().parseSignedClaims(token).getPayload();
    }

    public UUID extraireActeurId(String token) {
        return UUID.fromString(extraireClaims(token).getSubject());
    }

    /**
     * Le gateway (Web) émet un unique claim "role" (singulier) ; certains
     * autres émetteurs de tokens (service-ida, Mobile) émettent "roles"
     * (liste). On accepte les deux formes plutôt que de rejeter en 401
     * tout token gateway relayé — sans ce repli, aucun appel du gateway
     * vers ce service ne peut jamais s'authentifier.
     */
    @SuppressWarnings("unchecked")
    public List<String> extraireRoles(String token) {
        Claims claims = extraireClaims(token);
        Object roles = claims.get("roles");
        if (roles instanceof List<?>) {
            return (List<String>) roles;
        }
        String role = claims.get("role", String.class);
        return role != null ? List.of(role) : List.of();
    }

    public String extraireTenantId(String token) {
        return extraireClaims(token).get("tenantId", String.class);
    }
}
