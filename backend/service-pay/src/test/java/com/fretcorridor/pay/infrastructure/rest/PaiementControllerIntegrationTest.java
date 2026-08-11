package com.fretcorridor.pay.infrastructure.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PaiementControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void full_lifecycle_prise_en_charge_then_cloture_then_transporteur_can_read_its_own_ecriture() throws Exception {
        String missionId = "mission-e2e-" + System.nanoTime();
        String tenantId = "tenant-e2e-" + System.nanoTime();

        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/prise-en-charge", missionId))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/cloture", missionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId": "%s", "transporteurId": "actor-transporteur-1", "montant": 500, "referencePrestataire": "ref-1", "modePaiement": "VIREMENT"}
                                """.formatted(tenantId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nature").value("ENCAISSEMENT"));

        mockMvc.perform(get("/api/v1/pay/tenants/{tenantId}/rapport", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void cloture_without_prior_prise_en_charge_is_rejected_as_an_invalid_sequestre_transition() throws Exception {
        String missionId = "mission-e2e-" + System.nanoTime();

        // La clôture libère le séquestre : sans prise en charge préalable, aucun séquestre n'existe.
        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/cloture", missionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId": "tenant-1", "transporteurId": "actor-transporteur-1", "montant": 500, "referencePrestataire": "ref-1", "modePaiement": "VIREMENT"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void a_transporteur_never_sees_another_transporteur_s_ecritures() throws Exception {
        String missionA = "mission-a-" + System.nanoTime();
        String missionB = "mission-b-" + System.nanoTime();

        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/prise-en-charge", missionA));
        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/cloture", missionA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\": \"tenant-1\", \"transporteurId\": \"actor-transporteur-A\", \"montant\": 100, \"referencePrestataire\": \"ref-a\", \"modePaiement\": \"VIREMENT\"}"));
        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/reversement", missionA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\": \"tenant-1\", \"transporteurId\": \"actor-transporteur-A\", \"montant\": 90, \"referencePrestataire\": \"ref-a-rev\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/prise-en-charge", missionB));
        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/cloture", missionB)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\": \"tenant-1\", \"transporteurId\": \"actor-transporteur-B\", \"montant\": 200, \"referencePrestataire\": \"ref-b\", \"modePaiement\": \"MONNAIE_ELECTRONIQUE\"}"));
        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/reversement", missionB)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\": \"tenant-1\", \"transporteurId\": \"actor-transporteur-B\", \"montant\": 180, \"referencePrestataire\": \"ref-b-rev\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/pay/transporteurs/{transporteurId}/ecritures", "actor-transporteur-A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].montant").value(90));
    }

    /** EF-PAY-06 (terme contractuel), CDC UC-PAY-01 A1 : reversement sur garantie, sans aucun encaissement réel. */
    @Test
    void a_transporteur_is_reversed_against_a_garantie_with_no_prior_encaissement() throws Exception {
        String missionId = "mission-terme-" + System.nanoTime();
        String tenantId = "tenant-terme-" + System.nanoTime();

        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/garantie", missionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId": "%s", "garantId": "garant-bnp", "montant": 300, "referenceGarantie": "ref-garantie-1"}
                                """.formatted(tenantId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.garantId").value("garant-bnp"));

        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/reversement", missionId)
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\": \"tenant-1\", \"garantId\": \"garant-bnp\", \"montant\": 300, \"referenceGarantie\": \"ref-garantie-1\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/garantie", missionId)
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId": "%s", "montant": 150}
                                """.formatted(tenantId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.protectionAssuree").value(false));

        mockMvc.perform(get("/api/v1/pay/tenants/{tenantId}/paiements-especes", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].protectionAssuree").value(false));
    }

    @Test
    void a_cash_payment_never_unlocks_a_reversement_through_the_http_api() throws Exception {
        String missionId = "mission-especes-" + System.nanoTime();
        String tenantId = "tenant-especes-" + System.nanoTime();

        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/paiement-especes", missionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId": "%s", "montant": 150}
                                """.formatted(tenantId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/reversement", missionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId": "%s", "transporteurId": "actor-transporteur-1", "montant": 150, "referencePrestataire": "ref-rev"}
                                """.formatted(tenantId)))
                .andExpect(status().isConflict());
    }
}
