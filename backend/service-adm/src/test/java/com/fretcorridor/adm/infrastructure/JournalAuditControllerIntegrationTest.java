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
 * IDOR corrigé (audit CDC §7.2, "export cross-tenant si tenantId omis") :
 * un tenant sans rôle ADMINISTRATION ne doit plus pouvoir lire/exporter le
 * journal d'un autre tenant, ni obtenir tous les tenants en omettant le
 * paramètre.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class JournalAuditControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Value("${fretcorridor.jwt.secret}")
    private String jwtSecret;

    private String token(String tenantId) {
        return token(tenantId, List.of("ADMIN"));
    }

    private String tokenAdmin(String tenantId) {
        return token(tenantId, List.of("ADMINISTRATION"));
    }

    private String token(String tenantId, List<String> roles) {
        return Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("roles", roles)
                .claim("tenantId", tenantId)
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                .compact();
    }

    private void enregistrerEntree(String tenantId) throws Exception {
        mockMvc.perform(post("/api/v1/journal-audit")
                        .header("Authorization", "Bearer " + token(tenantId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId": "%s", "acteurId": "actor-1", "action": "TEST_ACTION", "ressource": "res-1"}
                                """.formatted(tenantId)))
                .andExpect(status().isCreated());
    }

    @Test
    void un_tenant_lit_uniquement_son_propre_journal_meme_sans_parametre() throws Exception {
        String tenantA = "tenant-e2e-" + System.nanoTime();
        String tenantB = "tenant-e2e-" + (System.nanoTime() + 1);
        enregistrerEntree(tenantA);
        enregistrerEntree(tenantB);

        mockMvc.perform(get("/api/v1/journal-audit")
                        .header("Authorization", "Bearer " + token(tenantA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.tenantId == '" + tenantA + "')]").exists())
                .andExpect(jsonPath("$[?(@.tenantId == '" + tenantB + "')]").doesNotExist());
    }

    @Test
    void un_tenant_ne_peut_pas_lire_le_journal_d_un_autre_tenant() throws Exception {
        String tenantA = "tenant-e2e-" + System.nanoTime();
        String tenantB = "tenant-e2e-" + (System.nanoTime() + 1);
        enregistrerEntree(tenantB);

        mockMvc.perform(get("/api/v1/journal-audit").param("tenantId", tenantB)
                        .header("Authorization", "Bearer " + token(tenantA)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/journal-audit/export").param("tenantId", tenantB)
                        .header("Authorization", "Bearer " + token(tenantA)))
                .andExpect(status().isForbidden());
    }

    @Test
    void administration_lit_tous_les_tenants_sans_parametre() throws Exception {
        String tenantA = "tenant-e2e-" + System.nanoTime();
        String tenantB = "tenant-e2e-" + (System.nanoTime() + 1);
        enregistrerEntree(tenantA);
        enregistrerEntree(tenantB);

        mockMvc.perform(get("/api/v1/journal-audit")
                        .header("Authorization", "Bearer " + tokenAdmin(tenantA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.tenantId == '" + tenantA + "')]").exists())
                .andExpect(jsonPath("$[?(@.tenantId == '" + tenantB + "')]").exists());
    }
}
