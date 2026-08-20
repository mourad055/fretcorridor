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

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Suite d'intégration bout-en-bout (Testcontainers, contexte Spring réel) —
 * authentification requise sur tous les endpoints (audit CDC §Transverse,
 * "8 services sans authentification") : chaque appel joint désormais un JWT
 * valide via {@link #token()}. Le contenu du token (acteurId, rôle) n'est
 * volontairement pas vérifié contre tenantId/transporteurId du corps de
 * requête ici — seule l'authentification est couverte par ce correctif, cf.
 * commentaire de {@code SecurityConfig}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PaiementControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Value("${fretcorridor.jwt.secret}")
    private String jwtSecret;

    private String token() {
        return token("tenant-jwt-test");
    }

    // tenantId vient désormais du JWT, jamais du corps de requête (audit CDC
    // §Transverse) - ce helper permet à chaque test de faire correspondre le
    // tenant du token à celui qu'il vérifie ensuite via un GET
    // /tenants/{id}/... ou /transporteurs/{id}/....
    private String token(String tenantId) {
        return Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("roles", List.of("BUREAU"))
                .claim("tenantId", tenantId)
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                .compact();
    }

    private String token(String tenantId, String acteurId) {
        return Jwts.builder()
                .subject(acteurId)
                .claim("roles", List.of("BUREAU"))
                .claim("tenantId", tenantId)
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                .compact();
    }

    private String tokenAdmin() {
        return Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("roles", List.of("ADMINISTRATION"))
                .claim("tenantId", "tenant-jwt-test")
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                .compact();
    }

    @Test
    void full_lifecycle_prise_en_charge_then_cloture_then_transporteur_can_read_its_own_ecriture() throws Exception {
        String missionId = "mission-e2e-" + System.nanoTime();
        String tenantId = "tenant-e2e-" + System.nanoTime();

        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/prise-en-charge", missionId)
                        .header("Authorization", "Bearer " + token()))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/cloture", missionId)
                        .header("Authorization", "Bearer " + token(tenantId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"transporteurId": "actor-transporteur-1", "montant": 500, "referencePrestataire": "ref-1", "modePaiement": "VIREMENT", "preuveLivraisonReference": "preuve-1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nature").value("ENCAISSEMENT"));

        mockMvc.perform(get("/api/v1/pay/tenants/{tenantId}/rapport", tenantId)
                        .header("Authorization", "Bearer " + token(tenantId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void cloture_without_prior_prise_en_charge_is_rejected_as_an_invalid_sequestre_transition() throws Exception {
        String missionId = "mission-e2e-" + System.nanoTime();

        // La clôture libère le séquestre : sans prise en charge préalable, aucun séquestre n'existe.
        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/cloture", missionId)
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId": "tenant-1", "transporteurId": "actor-transporteur-1", "montant": 500, "referencePrestataire": "ref-1", "modePaiement": "VIREMENT", "preuveLivraisonReference": "preuve-1"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void a_transporteur_never_sees_another_transporteur_s_ecritures() throws Exception {
        String missionA = "mission-a-" + System.nanoTime();
        String missionB = "mission-b-" + System.nanoTime();

        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/prise-en-charge", missionA)
                        .header("Authorization", "Bearer " + token()));
        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/cloture", missionA)
                        .header("Authorization", "Bearer " + token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\": \"tenant-1\", \"transporteurId\": \"actor-transporteur-A\", \"montant\": 100, \"referencePrestataire\": \"ref-a\", \"modePaiement\": \"VIREMENT\", \"preuveLivraisonReference\": \"preuve-a\"}"));
        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/reversement", missionA)
                        .header("Authorization", "Bearer " + token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\": \"tenant-1\", \"transporteurId\": \"actor-transporteur-A\", \"montant\": 90, \"referencePrestataire\": \"ref-a-rev\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/prise-en-charge", missionB)
                        .header("Authorization", "Bearer " + token()));
        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/cloture", missionB)
                        .header("Authorization", "Bearer " + token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\": \"tenant-1\", \"transporteurId\": \"actor-transporteur-B\", \"montant\": 200, \"referencePrestataire\": \"ref-b\", \"modePaiement\": \"MONNAIE_ELECTRONIQUE\", \"preuveLivraisonReference\": \"preuve-b\"}"));
        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/reversement", missionB)
                        .header("Authorization", "Bearer " + token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\": \"tenant-1\", \"transporteurId\": \"actor-transporteur-B\", \"montant\": 180, \"referencePrestataire\": \"ref-b-rev\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/pay/transporteurs/{transporteurId}/ecritures", "actor-transporteur-A")
                        .header("Authorization", "Bearer " + tokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].montant").value(90));
    }

    /** EF-PAY-06 (terme contractuel), CDC UC-PAY-01 A1 : reversement sur garantie, sans aucun encaissement réel. */
    @Test
    void a_transporteur_is_reversed_against_a_garantie_with_no_prior_encaissement() throws Exception {
        String missionId = "mission-terme-" + System.nanoTime();
        String tenantId = "tenant-terme-" + System.nanoTime();

        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/prise-en-charge", missionId)
                        .header("Authorization", "Bearer " + token()))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/confirmation-livraison", missionId)
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId": "%s", "transporteurId": "actor-transporteur-1", "preuveLivraisonReference": "preuve-terme-1"}
                                """.formatted(tenantId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/garantie", missionId)
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId": "%s", "garantId": "garant-bnp", "montant": 300, "referenceGarantie": "ref-garantie-1"}
                                """.formatted(tenantId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.garantId").value("garant-bnp"));

        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/reversement", missionId)
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId": "%s", "transporteurId": "actor-transporteur-1", "montant": 300, "referencePrestataire": "ref-rev-terme"}
                                """.formatted(tenantId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nature").value("REVERSEMENT"));
    }

    @Test
    void souscribing_a_second_garantie_for_the_same_mission_is_rejected() throws Exception {
        String missionId = "mission-terme-" + System.nanoTime();

        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/garantie", missionId)
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\": \"tenant-1\", \"garantId\": \"garant-bnp\", \"montant\": 300, \"referenceGarantie\": \"ref-garantie-1\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/garantie", missionId)
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\": \"tenant-1\", \"garantId\": \"garant-bnp\", \"montant\": 300, \"referenceGarantie\": \"ref-garantie-2\"}"))
                .andExpect(status().isConflict());
    }

    /** EF-PAY-07 (S) : la déclaration espèces signale explicitement l'absence de protection. */
    @Test
    void a_cash_payment_declaration_explicitly_signals_no_protection() throws Exception {
        String missionId = "mission-especes-" + System.nanoTime();
        String tenantId = "tenant-especes-" + System.nanoTime();

        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/paiement-especes", missionId)
                        .header("Authorization", "Bearer " + token(tenantId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"montant": 150}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.protectionAssuree").value(false));

        mockMvc.perform(get("/api/v1/pay/tenants/{tenantId}/paiements-especes", tenantId)
                        .header("Authorization", "Bearer " + token(tenantId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].protectionAssuree").value(false));
    }

    @Test
    void a_cash_payment_never_unlocks_a_reversement_through_the_http_api() throws Exception {
        String missionId = "mission-especes-" + System.nanoTime();
        String tenantId = "tenant-especes-" + System.nanoTime();

        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/paiement-especes", missionId)
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId": "%s", "montant": 150}
                                """.formatted(tenantId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/reversement", missionId)
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId": "%s", "transporteurId": "actor-transporteur-1", "montant": 150, "referencePrestataire": "ref-rev"}
                                """.formatted(tenantId)))
                .andExpect(status().isConflict());
    }

    /** EF-PAY-06, Item B : le chargeur choisit son moyen de paiement avant tout encaissement (CDC UC-PAY-01 étape 2). */
    @Test
    void a_chargeur_chooses_a_moyen_de_paiement_and_it_is_readable_before_any_encaissement() throws Exception {
        String missionId = "mission-choix-" + System.nanoTime();
        String tenantId = "tenant-choix-" + System.nanoTime();

        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/moyen-paiement", missionId)
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId": "%s", "modePaiement": "VIREMENT"}
                                """.formatted(tenantId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.modePaiement").value("VIREMENT"));

        mockMvc.perform(get("/api/v1/pay/missions/{missionId}/moyen-paiement", missionId)
                        .header("Authorization", "Bearer " + token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modePaiement").value("VIREMENT"));
    }

    @Test
    void choosing_a_moyen_de_paiement_twice_for_the_same_mission_is_rejected() throws Exception {
        String missionId = "mission-choix-" + System.nanoTime();
        String tenantId = "tenant-choix-" + System.nanoTime();

        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/moyen-paiement", missionId)
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId": "%s", "modePaiement": "MONNAIE_ELECTRONIQUE"}
                                """.formatted(tenantId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/moyen-paiement", missionId)
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId": "%s", "modePaiement": "VIREMENT"}
                                """.formatted(tenantId)))
                .andExpect(status().isConflict());
    }

    @Test
    void reading_a_moyen_de_paiement_never_chosen_returns_404() throws Exception {
        mockMvc.perform(get("/api/v1/pay/missions/{missionId}/moyen-paiement", "mission-jamais-choisie")
                        .header("Authorization", "Bearer " + token()))
                .andExpect(status().isNotFound());
    }

    /** IDOR corrigé (audit CDC §Transverse) : tenantId vient du JWT, jamais du corps — un tenant ne lit pas le rapport d'un autre. */
    @Test
    void reading_another_tenant_s_rapport_is_refused() throws Exception {
        String tenantId = "tenant-e2e-" + System.nanoTime();
        String autreTenantId = "tenant-autre-" + System.nanoTime();

        mockMvc.perform(get("/api/v1/pay/tenants/{tenantId}/rapport", tenantId)
                        .header("Authorization", "Bearer " + token(autreTenantId)))
                .andExpect(status().isForbidden());
    }

    /** IDOR corrigé (audit CDC §Transverse) : ADMINISTRATION consulte n'importe quel tenant, consultation transverse légitime. */
    @Test
    void administration_reads_any_tenant_s_rapport() throws Exception {
        String tenantId = "tenant-e2e-" + System.nanoTime();

        mockMvc.perform(get("/api/v1/pay/tenants/{tenantId}/rapport", tenantId)
                        .header("Authorization", "Bearer " + tokenAdmin()))
                .andExpect(status().isOk());
    }

    /** IDOR corrigé (audit CDC §Transverse) : un transporteur ne lit pas les écritures d'un autre transporteur que lui-même. */
    @Test
    void reading_another_transporteur_s_ecritures_is_refused() throws Exception {
        mockMvc.perform(get("/api/v1/pay/transporteurs/{transporteurId}/ecritures", "actor-transporteur-A")
                        .header("Authorization", "Bearer " + token("tenant-jwt-test", "actor-transporteur-B")))
                .andExpect(status().isForbidden());
    }

    /** IDOR corrigé (audit CDC §Transverse) : un transporteur lit ses propres écritures sans restriction. */
    @Test
    void reading_ones_own_ecritures_is_allowed() throws Exception {
        mockMvc.perform(get("/api/v1/pay/transporteurs/{transporteurId}/ecritures", "actor-transporteur-A")
                        .header("Authorization", "Bearer " + token("tenant-jwt-test", "actor-transporteur-A")))
                .andExpect(status().isOk());
    }
}
