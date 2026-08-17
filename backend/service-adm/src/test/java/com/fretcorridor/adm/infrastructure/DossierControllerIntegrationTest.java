package com.fretcorridor.adm.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DoD PRD §9 S10 : un admin traite un dossier de bout en bout, décision journalisée.
 * {@code max.block.ms} raccourci : aucun broker Kafka réel ici, sans quoi
 * chaque dossier LITIGE publié via {@code KafkaDossierEventPublisher} bloque
 * jusqu'à 60s (comportement de dégradation gracieuse inchangé — toujours
 * catché, jamais propagé, cf. ENF-DIS-04).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestPropertySource(properties = "spring.kafka.producer.properties.max.block.ms=2000")
class DossierControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void full_lifecycle_ouverture_prise_en_charge_puis_decision_est_journalise() throws Exception {
        String tenantId = "tenant-e2e-" + System.nanoTime();
        String delai = Instant.now().plus(2, ChronoUnit.DAYS).toString();

        String reponseOuverture = mockMvc.perform(post("/api/v1/dossiers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId": "%s", "type": "LITIGE", "priorite": "NORMALE", "missionId": "mission-a",
                                 "parties": ["acteur-transporteur-1"], "preuvesReferences": [], "delaiTraitement": "%s"}
                                """.formatted(tenantId, delai)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("OUVERT"))
                .andReturn().getResponse().getContentAsString();

        String dossierId = reponseOuverture.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
        definirGrille(tenantId);

        mockMvc.perform(post("/api/v1/dossiers/{id}/prise-en-charge", dossierId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acteurId\": \"actor-admin-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("EN_COURS"));

        mockMvc.perform(post("/api/v1/dossiers/{id}/decision", dossierId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decision": "RESOLU_EN_FAVEUR_TRANSPORTEUR", "motif": "Preuve conforme", "acteurId": "actor-admin-1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("CLOS"))
                .andExpect(jsonPath("$.decision").value("RESOLU_EN_FAVEUR_TRANSPORTEUR"));

        mockMvc.perform(get("/api/v1/journal-audit").param("tenantId", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.action == 'DOSSIER_DECISION_RESOLU_EN_FAVEUR_TRANSPORTEUR')]").exists());
    }

    @Test
    void decision_sur_un_dossier_deja_clos_est_rejetee_avec_un_conflit() throws Exception {
        String tenantId = "tenant-e2e-" + System.nanoTime();
        String delai = Instant.now().plus(1, ChronoUnit.DAYS).toString();

        String reponseOuverture = mockMvc.perform(post("/api/v1/dossiers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId": "%s", "type": "INCIDENT", "priorite": "BASSE", "delaiTraitement": "%s"}
                                """.formatted(tenantId, delai)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String dossierId = reponseOuverture.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
        definirGrille(tenantId);

        mockMvc.perform(post("/api/v1/dossiers/{id}/decision", dossierId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\": \"CLOS_SANS_SUITE\", \"motif\": \"m\", \"acteurId\": \"actor-admin-1\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/dossiers/{id}/decision", dossierId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\": \"AUTRE\", \"motif\": \"m\", \"acteurId\": \"actor-admin-1\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void un_dossier_avec_delai_depasse_est_escalade_via_l_endpoint_dedie() throws Exception {
        String tenantId = "tenant-e2e-" + System.nanoTime();
        String delaiDepasse = Instant.now().minus(1, ChronoUnit.HOURS).toString();

        mockMvc.perform(post("/api/v1/dossiers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId": "%s", "type": "LITIGE", "priorite": "BASSE", "delaiTraitement": "%s"}
                                """.formatted(tenantId, delaiDepasse)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/dossiers/escalade"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/dossiers").param("tenantId", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].statut").value("ESCALADE"))
                .andExpect(jsonPath("$[0].priorite").value("HAUTE"));
    }

    /** RG-096 (CDC) : un opérateur ne doit jamais avoir à trancher sans grille. */
    @Test
    void decider_sans_grille_de_decision_definie_est_rejete_avec_un_conflit() throws Exception {
        String tenantId = "tenant-e2e-" + System.nanoTime();
        String delai = Instant.now().plus(1, ChronoUnit.DAYS).toString();

        String reponseOuverture = mockMvc.perform(post("/api/v1/dossiers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId": "%s", "type": "INCIDENT", "priorite": "BASSE", "delaiTraitement": "%s"}
                                """.formatted(tenantId, delai)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String dossierId = reponseOuverture.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(post("/api/v1/dossiers/{id}/decision", dossierId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\": \"CLOS_SANS_SUITE\", \"motif\": \"m\", \"acteurId\": \"actor-admin-1\"}"))
                .andExpect(status().isConflict());
    }

    private void definirGrille(String tenantId) throws Exception {
        mockMvc.perform(put("/api/v1/configurations/{cle}", "grille-decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"perimetre": "%s", "valeur": "grille v1", "auteur": "actor-admin-1"}
                                """.formatted(tenantId)))
                .andExpect(status().isOk());
    }
}
