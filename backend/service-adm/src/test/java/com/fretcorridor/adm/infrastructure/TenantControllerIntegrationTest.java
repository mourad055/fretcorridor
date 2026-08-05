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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FE-ADM-04 : gestion des tenants. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TenantControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void creer_un_tenant_puis_le_retrouver_dans_la_liste() throws Exception {
        String tenantId = "tenant-e2e-" + System.nanoTime();

        mockMvc.perform(post("/api/v1/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id": "%s", "nom": "Bureau de fret Test", "pays": "Cameroun", "auteur": "actor-admin-1"}
                                """.formatted(tenantId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/tenants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + tenantId + "')]").exists());
    }

    @Test
    void creer_deux_tenants_avec_le_meme_identifiant_est_rejete_avec_un_conflit() throws Exception {
        String tenantId = "tenant-e2e-" + System.nanoTime();
        String body = """
                {"id": "%s", "nom": "Bureau", "pays": "Cameroun", "auteur": "actor-admin-1"}
                """.formatted(tenantId);

        mockMvc.perform(post("/api/v1/tenants").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/tenants").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }
}
