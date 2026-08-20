package com.fretcorridor.trk.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.List;
import java.util.UUID;

// service-trk n'expose aucun endpoint HTTP (que des listeners Kafka +
// ServiceOptClient sortant) - ce JwtService est ajoute par coherence de
// dependances entre les 4 modules du perimetre Moteur, sans usage actif
// aujourd'hui. Prepare le terrain si un endpoint de consultation apparait
// plus tard (Phase 2+).
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

    @SuppressWarnings("unchecked")
    public List<String> extraireRoles(String token) {
        return (List<String>) extraireClaims(token).get("roles");
    }

    public String extraireTenantId(String token) {
        return extraireClaims(token).get("tenantId", String.class);
    }
}
