package com.fretcorridor.adm.infrastructure;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FE-ADM-03 : chaque redéfinition crée une nouvelle version, jamais une modification en place. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ConfigurationControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void redefinir_une_configuration_deux_fois_produit_deux_versions_et_la_derniere_est_courante() throws Exception {
        String cle = "seuil-agregation-bur-" + System.nanoTime();

        mockMvc.perform(put("/api/v1/configurations/{cle}", cle)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valeur\": \"3\", \"auteur\": \"actor-admin-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(put("/api/v1/configurations/{cle}", cle)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valeur\": \"5\", \"auteur\": \"actor-admin-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2));

        mockMvc.perform(get("/api/v1/configurations/{cle}", cle))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valeur").value("5"));

        mockMvc.perform(get("/api/v1/configurations/{cle}/historique", cle))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    /** EF-ADM-06 : le catalogue liste les clés déjà configurées, sans avoir à connaître leur nom à l'avance. */
    @Test
    void le_catalogue_liste_les_cles_deja_configurees_avec_leur_valeur_courante() throws Exception {
        String cle = "seuil-agregation-bur-" + System.nanoTime();

        mockMvc.perform(put("/api/v1/configurations/{cle}", cle)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valeur\": \"3\", \"auteur\": \"actor-admin-1\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/configurations/{cle}", cle)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valeur\": \"5\", \"auteur\": \"actor-admin-1\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/configurations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.cle=='" + cle + "')].valeur").value("5"))
                .andExpect(jsonPath("$[?(@.cle=='" + cle + "')].version").value(2));
    }
}
