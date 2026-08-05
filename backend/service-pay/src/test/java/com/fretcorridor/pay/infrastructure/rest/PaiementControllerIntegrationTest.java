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
                                {"tenantId": "%s", "transporteurId": "actor-transporteur-1", "montant": 500, "referencePrestataire": "ref-1"}
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
                                {"tenantId": "tenant-1", "transporteurId": "actor-transporteur-1", "montant": 500, "referencePrestataire": "ref-1"}
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
                .content("{\"tenantId\": \"tenant-1\", \"transporteurId\": \"actor-transporteur-A\", \"montant\": 100, \"referencePrestataire\": \"ref-a\"}"));
        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/reversement", missionA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\": \"tenant-1\", \"transporteurId\": \"actor-transporteur-A\", \"montant\": 90, \"referencePrestataire\": \"ref-a-rev\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/prise-en-charge", missionB));
        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/cloture", missionB)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\": \"tenant-1\", \"transporteurId\": \"actor-transporteur-B\", \"montant\": 200, \"referencePrestataire\": \"ref-b\"}"));
        mockMvc.perform(post("/api/v1/pay/missions/{missionId}/reversement", missionB)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\": \"tenant-1\", \"transporteurId\": \"actor-transporteur-B\", \"montant\": 180, \"referencePrestataire\": \"ref-b-rev\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/pay/transporteurs/{transporteurId}/ecritures", "actor-transporteur-A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].montant").value(90));
    }
}
