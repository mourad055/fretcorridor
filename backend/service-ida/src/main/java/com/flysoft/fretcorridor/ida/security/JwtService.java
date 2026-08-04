package com.flysoft.fretcorridor.ida.security;

import com.flysoft.fretcorridor.ida.entity.Acteur;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JwtService {

    @Value("${fretcorridor.jwt.secret}")
    private String secret;

    @Value("${fretcorridor.jwt.expiration-access-ms}")
    private long expirationAccessMs;

    @Value("${fretcorridor.jwt.expiration-refresh-ms}")
    private long expirationRefreshMs;

    private SecretKey cle() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String genererAccessToken(Acteur acteur) {
        List<String> roles = acteur.getRoles().stream().map(Enum::name).collect(Collectors.toList());
        return Jwts.builder()
                .subject(acteur.getId().toString())
                .claim("telephone", acteur.getTelephone())
                .claim("roles", roles)
                .claim("tenantId", acteur.getTenantId())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationAccessMs))
                .signWith(cle())
                .compact();
    }

    public String genererRefreshToken(Acteur acteur) {
        return Jwts.builder()
                .subject(acteur.getId().toString())
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationRefreshMs))
                .signWith(cle())
                .compact();
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
