package com.fretcorridor.pay.infrastructure.rest;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * EF-PAY-05, bout en bout HTTP + base réelle : une notification correctement
 * signée crée un encaissement ; une signature invalide est rejetée sans rien
 * écrire ; un rejeu (même clé d'idempotence) n'écrit pas une seconde fois.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class WebhookPrestataireControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Value("${fretcorridor.pay.webhook-secret}")
    private String secret;

    @Value("${fretcorridor.jwt.secret}")
    private String jwtSecret;

    // /webhooks/prestataire reste permitAll (le prestataire externe n'a pas de
    // JWT), mais GET /tenants/{id}/rapport (utilisé ici pour vérifier l'état
    // écrit) exige désormais une authentification comme tout autre endpoint.
    // Le tenantId créé par le webhook étant dynamique (généré par test), on
    // consulte avec un rôle ADMINISTRATION (consultation transverse
    // légitime, audit CDC §Transverse) plutôt que de faire correspondre le
    // tenant du token - ces tests portent sur l'idempotence/la signature,
    // pas sur le filtrage tenant.
    private String tokenAdmin() {
        return Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("roles", List.of("ADMINISTRATION"))
                .claim("tenantId", "tenant-jwt-test")
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                .compact();
    }

    @Test
    void a_correctly_signed_new_notification_records_an_encaissement() throws Exception {
        String tenantId = "tenant-webhook-" + System.nanoTime();
        String missionId = "mission-webhook-" + System.nanoTime();
        String corps = corps(tenantId, missionId, "100", "ref-1");

        mockMvc.perform(post("/api/v1/pay/webhooks/prestataire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Prestataire-Signature", hmac(corps))
                        .header("X-Prestataire-Idempotency-Key", "idem-" + missionId)
                        .content(corps))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/pay/tenants/{tenantId}/rapport", tenantId)
                        .header("Authorization", "Bearer " + tokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nature").value("ENCAISSEMENT"))
                .andExpect(jsonPath("$[0].modePaiement").value("VIREMENT"));
    }

    @Test
    void a_notification_with_an_invalid_signature_is_rejected_and_records_nothing() throws Exception {
        String tenantId = "tenant-webhook-" + System.nanoTime();
        String missionId = "mission-webhook-" + System.nanoTime();
        String corps = corps(tenantId, missionId, "100", "ref-1");

        mockMvc.perform(post("/api/v1/pay/webhooks/prestataire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Prestataire-Signature", "0000000000000000000000000000000000000000000000000000000000000000")
                        .header("X-Prestataire-Idempotency-Key", "idem-" + missionId)
                        .content(corps))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/pay/tenants/{tenantId}/rapport", tenantId)
                        .header("Authorization", "Bearer " + tokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void replaying_the_same_idempotency_key_does_not_duplicate_the_encaissement() throws Exception {
        String tenantId = "tenant-webhook-" + System.nanoTime();
        String missionId = "mission-webhook-" + System.nanoTime();
        String corps = corps(tenantId, missionId, "100", "ref-1");
        String idempotenceKey = "idem-" + missionId;

        mockMvc.perform(post("/api/v1/pay/webhooks/prestataire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Prestataire-Signature", hmac(corps))
                        .header("X-Prestataire-Idempotency-Key", idempotenceKey)
                        .content(corps))
                .andExpect(status().isOk());

        // Rejeu réseau typique d'un prestataire de paiement : même clé, même corps.
        mockMvc.perform(post("/api/v1/pay/webhooks/prestataire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Prestataire-Signature", hmac(corps))
                        .header("X-Prestataire-Idempotency-Key", idempotenceKey)
                        .content(corps))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/pay/tenants/{tenantId}/rapport", tenantId)
                        .header("Authorization", "Bearer " + tokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    private static String corps(String tenantId, String missionId, String montant, String reference) {
        return """
                {"tenantId": "%s", "missionId": "%s", "montant": %s, "referencePrestataire": "%s", "modePaiement": "VIREMENT"}
                """.formatted(tenantId, missionId, montant, reference).strip();
    }

    private String hmac(String corps) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(corps.getBytes(StandardCharsets.UTF_8)));
    }
}
