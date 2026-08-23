package com.fretcorridor.adm.infrastructure;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

import java.util.List;
import java.util.UUID;

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

    @Value("${fretcorridor.jwt.secret}")
    private String jwtSecret;

    private String token() {
        return token("tenant-jwt-test");
    }

    private String token(String tenantId) {
        return token(tenantId, UUID.randomUUID().toString());
    }

    private String token(String tenantId, String acteurId) {
        return Jwts.builder()
                .subject(acteurId)
                .claim("roles", List.of("ADMIN"))
                .claim("tenantId", tenantId)
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                .compact();
    }

    /** IDOR corrigé (audit CDC du 19 août, §7.2) : un acteur consulte un dossier de son propre tenant. */
    @Test
    void consulter_un_dossier_de_son_propre_tenant_reussit() throws Exception {
        String delai = Instant.now().plus(1, ChronoUnit.DAYS).toString();

        String reponseOuverture = mockMvc.perform(post("/api/v1/dossiers")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId": "tenant-jwt-test", "type": "INCIDENT", "priorite": "BASSE", "delaiTraitement": "%s"}
                                """.formatted(delai)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String dossierId = reponseOuverture.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/v1/dossiers/{id}", dossierId)
                        .header("Authorization", "Bearer " + token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(dossierId));
    }

    /**
     * S19 (audit de suivi, 23 août) : un chargeur (Mobile, app_client) qui
     * signale un litige n'envoie ni motif/description structurés côté ADM
     * ni délai de traitement administratif - le motif/description doivent
     * survivre, et un délai par défaut (72h, DossierController.DELAI_TRAITEMENT_DEFAUT)
     * doit être appliqué plutôt que de rejeter la requête.
     */
    @Test
    void ouvrir_un_litige_sans_delai_de_traitement_applique_le_delai_par_defaut() throws Exception {
        String tenantId = "tenant-e2e-" + System.nanoTime();
        Instant avant = Instant.now();

        mockMvc.perform(post("/api/v1/dossiers")
                        .header("Authorization", "Bearer " + token(tenantId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId": "%s", "type": "LITIGE", "priorite": "NORMALE", "missionId": "mission-a",
                                 "motif": "Marchandise endommagée", "description": "Le carton était écrasé."}
                                """.formatted(tenantId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.motif").value("Marchandise endommagée"))
                .andExpect(jsonPath("$.description").value("Le carton était écrasé."))
                .andExpect(jsonPath("$.delaiTraitement").exists());

        mockMvc.perform(get("/api/v1/dossiers")
                        .header("Authorization", "Bearer " + token(tenantId)).param("tenantId", tenantId))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String corps = result.getResponse().getContentAsString();
                    String delaiTexte = corps.replaceAll(".*\"delaiTraitement\":\"([^\"]+)\".*", "$1");
                    Instant delai = Instant.parse(delaiTexte);
                    // Doit tomber proche de "maintenant + 72h", jamais null/immediat.
                    org.assertj.core.api.Assertions.assertThat(delai)
                            .isAfter(avant.plus(71, ChronoUnit.HOURS))
                            .isBefore(avant.plus(73, ChronoUnit.HOURS));
                });
    }

    /** IDOR corrigé (audit CDC du 19 août, §7.2) : un acteur d'un autre tenant ne peut pas consulter ce dossier. */
    @Test
    void consulter_un_dossier_d_un_autre_tenant_retourne_404() throws Exception {
        String delai = Instant.now().plus(1, ChronoUnit.DAYS).toString();

        String reponseOuverture = mockMvc.perform(post("/api/v1/dossiers")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId": "tenant-jwt-test", "type": "INCIDENT", "priorite": "BASSE", "delaiTraitement": "%s"}
                                """.formatted(delai)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String dossierId = reponseOuverture.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/v1/dossiers/{id}", dossierId)
                        .header("Authorization", "Bearer " + token("tenant-autre")))
                .andExpect(status().isNotFound());
    }

    @Test
    void full_lifecycle_ouverture_prise_en_charge_puis_decision_est_journalise() throws Exception {
        String tenantId = "tenant-e2e-" + System.nanoTime();
        String delai = Instant.now().plus(2, ChronoUnit.DAYS).toString();

        String reponseOuverture = mockMvc.perform(post("/api/v1/dossiers")
                        .header("Authorization", "Bearer " + token(tenantId))
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
                        .header("Authorization", "Bearer " + token(tenantId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("EN_COURS"));

        mockMvc.perform(post("/api/v1/dossiers/{id}/decision", dossierId)
                        .header("Authorization", "Bearer " + token(tenantId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decision": "RESOLU_EN_FAVEUR_TRANSPORTEUR", "motif": "Preuve conforme"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("CLOS"))
                .andExpect(jsonPath("$.decision").value("RESOLU_EN_FAVEUR_TRANSPORTEUR"));

        mockMvc.perform(get("/api/v1/journal-audit")
                        .header("Authorization", "Bearer " + token(tenantId)).param("tenantId", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.action == 'DOSSIER_DECISION_RESOLU_EN_FAVEUR_TRANSPORTEUR')]").exists());
    }

    @Test
    void decision_sur_un_dossier_deja_clos_est_rejetee_avec_un_conflit() throws Exception {
        String tenantId = "tenant-e2e-" + System.nanoTime();
        String delai = Instant.now().plus(1, ChronoUnit.DAYS).toString();

        String reponseOuverture = mockMvc.perform(post("/api/v1/dossiers")
                        .header("Authorization", "Bearer " + token(tenantId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId": "%s", "type": "INCIDENT", "priorite": "BASSE", "delaiTraitement": "%s"}
                                """.formatted(tenantId, delai)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String dossierId = reponseOuverture.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
        definirGrille(tenantId);

        mockMvc.perform(post("/api/v1/dossiers/{id}/decision", dossierId)
                        .header("Authorization", "Bearer " + token(tenantId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\": \"CLOS_SANS_SUITE\", \"motif\": \"m\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/dossiers/{id}/decision", dossierId)
                        .header("Authorization", "Bearer " + token(tenantId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\": \"AUTRE\", \"motif\": \"m\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void un_dossier_avec_delai_depasse_est_escalade_via_l_endpoint_dedie() throws Exception {
        String tenantId = "tenant-e2e-" + System.nanoTime();
        String delaiDepasse = Instant.now().minus(1, ChronoUnit.HOURS).toString();

        mockMvc.perform(post("/api/v1/dossiers")
                        .header("Authorization", "Bearer " + token(tenantId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId": "%s", "type": "LITIGE", "priorite": "BASSE", "delaiTraitement": "%s"}
                                """.formatted(tenantId, delaiDepasse)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/dossiers/escalade")
                        .header("Authorization", "Bearer " + token(tenantId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/dossiers")
                        .header("Authorization", "Bearer " + token(tenantId)).param("tenantId", tenantId))
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
                        .header("Authorization", "Bearer " + token(tenantId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId": "%s", "type": "INCIDENT", "priorite": "BASSE", "delaiTraitement": "%s"}
                                """.formatted(tenantId, delai)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String dossierId = reponseOuverture.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(post("/api/v1/dossiers/{id}/decision", dossierId)
                        .header("Authorization", "Bearer " + token(tenantId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\": \"CLOS_SANS_SUITE\", \"motif\": \"m\"}"))
                .andExpect(status().isConflict());
    }

    /** EF-ADM-04/RG-098 : un recours ouvre un second Dossier, refusé au même opérateur que le premier décideur. */
    @Test
    void un_recours_ouvre_un_second_dossier_refuse_au_premier_decideur() throws Exception {
        String tenantId = "tenant-e2e-" + System.nanoTime();
        String delai = Instant.now().plus(1, ChronoUnit.DAYS).toString();
        definirGrille(tenantId);

        String reponseOuverture = mockMvc.perform(post("/api/v1/dossiers")
                        .header("Authorization", "Bearer " + token(tenantId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId": "%s", "type": "INCIDENT", "priorite": "BASSE", "delaiTraitement": "%s"}
                                """.formatted(tenantId, delai)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String dossierId = reponseOuverture.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(post("/api/v1/dossiers/{id}/decision", dossierId)
                        .header("Authorization", "Bearer " + token(tenantId, "actor-admin-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\": \"CLOS_SANS_SUITE\", \"motif\": \"m\"}"))
                .andExpect(status().isOk());

        String reponseRecours = mockMvc.perform(post("/api/v1/dossiers/{id}/recours", dossierId)
                        .header("Authorization", "Bearer " + token(tenantId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"priorite": "HAUTE", "delaiTraitement": "%s"}
                                """.formatted(delai)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recoursDeDossierId").value(dossierId))
                .andExpect(jsonPath("$.statut").value("OUVERT"))
                .andReturn().getResponse().getContentAsString();
        String recoursId = reponseRecours.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(post("/api/v1/dossiers/{id}/prise-en-charge", recoursId)
                        .header("Authorization", "Bearer " + token(tenantId, "actor-admin-1")))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/dossiers/{id}/prise-en-charge", recoursId)
                        .header("Authorization", "Bearer " + token(tenantId, "actor-admin-2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("EN_COURS"));
    }

    @Test
    void un_recours_sur_un_dossier_pas_encore_tranche_est_rejete_avec_un_conflit() throws Exception {
        String tenantId = "tenant-e2e-" + System.nanoTime();
        String delai = Instant.now().plus(1, ChronoUnit.DAYS).toString();

        String reponseOuverture = mockMvc.perform(post("/api/v1/dossiers")
                        .header("Authorization", "Bearer " + token(tenantId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId": "%s", "type": "INCIDENT", "priorite": "BASSE", "delaiTraitement": "%s"}
                                """.formatted(tenantId, delai)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String dossierId = reponseOuverture.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(post("/api/v1/dossiers/{id}/recours", dossierId)
                        .header("Authorization", "Bearer " + token(tenantId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"priorite": "HAUTE", "delaiTraitement": "%s"}
                                """.formatted(delai)))
                .andExpect(status().isConflict());
    }

    private void definirGrille(String tenantId) throws Exception {
        mockMvc.perform(put("/api/v1/configurations/{cle}", "grille-decision")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"perimetre": "%s", "valeur": "grille v1", "auteur": "actor-admin-1"}
                                """.formatted(tenantId)))
                .andExpect(status().isOk());
    }
}
