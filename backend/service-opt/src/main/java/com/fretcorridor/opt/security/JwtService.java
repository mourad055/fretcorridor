package com.fretcorridor.opt.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.List;
import java.util.UUID;

// service-opt ne delivre aucun token - il valide ceux emis par service-ida,
// avec le meme secret partage. AffectationL1Controller/FiltrageL0Controller
// restent des endpoints de test manuel (Sprint 5), jamais appeles avec JWT
// en flux nominal - le vrai declenchement passe par Kafka
// (DemandePubliee/CapaciteDeclaree listeners).
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
