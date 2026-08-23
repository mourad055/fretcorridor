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
        return genererAccessToken(acteur, acteur.getTenantId());
    }

    // S18 (audit de suivi, 23 aout) : tenantId EFFECTIF distinct du tenant
    // d'origine de l'acteur (Acteur.tenantId, jamais modifie) - reemis apres
    // une selection de tenant explicite (cf AffiliationService.selectionner),
    // uniquement si l'appelant a deja verifie l'affiliation. Le reste du
    // systeme (tous les autres services) continue de lire tenantId depuis ce
    // claim sans savoir qu'il differe potentiellement du tenant d'origine -
    // aucun changement necessaire ailleurs.
    public String genererAccessToken(Acteur acteur, String tenantIdEffectif) {
        List<String> roles = acteur.getRoles().stream().map(Enum::name).collect(Collectors.toList());
        return Jwts.builder()
                .subject(acteur.getId().toString())
                .claim("telephone", acteur.getTelephone())
                .claim("roles", roles)
                .claim("tenantId", tenantIdEffectif)
                .claim("niveauKyc", acteur.getNiveauKyc().name())
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

    public String extraireNiveauKyc(String token) {
        return extraireClaims(token).get("niveauKyc", String.class);
    }
}
