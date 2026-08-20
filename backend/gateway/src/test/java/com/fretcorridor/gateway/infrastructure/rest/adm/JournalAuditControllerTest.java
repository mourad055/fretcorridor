package com.fretcorridor.gateway.infrastructure.rest.adm;

import com.fretcorridor.gateway.domain.adm.AdmPort;
import com.fretcorridor.gateway.domain.adm.EntreeJournalAuditVue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** FE-ADM-05 (Sprint 10) : journal d'audit consultable et exportable, RBAC ADMIN. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
class JournalAuditControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private AdmPort admPort;

    private String tokenFor(String phone) {
        return webTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"phone\": \"" + phone + "\", \"code\": \"123456\"}")
                .exchange()
                .expectStatus().isOk()
                .returnResult(Map.class)
                .getResponseBody()
                .blockFirst()
                .get("token")
                .toString();
    }

    @Test
    void a_bureau_actor_cannot_reach_the_journal_audit_endpoint() {
        String token = tokenFor("+237600000001");

        webTestClient.get().uri("/api/v1/admin/journal-audit")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void an_admin_lists_the_journal_entries() {
        String token = tokenFor("+237600000003");
        when(admPort.journalAudit(any(), any())).thenReturn(Flux.just(
                new EntreeJournalAuditVue("e1", "tenant-bgft-douala", "actor-admin-1", "DOSSIER_OUVERT",
                        "dossier:d1", Instant.now())));

        webTestClient.get().uri("/api/v1/admin/journal-audit")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Object.class)
                .hasSize(1);
    }

    @Test
    void an_admin_exports_the_journal_as_csv() {
        String token = tokenFor("+237600000003");
        when(admPort.exporterJournalAudit(any(), any())).thenReturn(Mono.just("id,tenantId,acteurId,action,ressource,horodatage\n"));

        webTestClient.get().uri("/api/v1/admin/journal-audit/export")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("text/csv");
    }
}
