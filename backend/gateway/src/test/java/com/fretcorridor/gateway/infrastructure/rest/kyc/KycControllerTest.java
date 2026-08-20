package com.fretcorridor.gateway.infrastructure.rest.kyc;

import com.fretcorridor.gateway.domain.adm.AdmPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FE-ADM-06 + ENF-SEC-02 : dashboard KYC, RBAC et journalisation des décisions.
 * DirtiesContext par méthode : l'adaptateur mock est un singleton avec état mutable
 * (dossiers) — chaque test doit repartir des 3 dossiers d'amorçage, sans
 * dépendre de l'ordre d'exécution ni des autres classes de test partageant le contexte.
 * Le journal d'audit réel appelle service-adm par HTTP — remplacé ici par un
 * double, comme PayReadPort dans les tests Sprint 8.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class KycControllerTest {

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
    void an_admin_sees_the_pending_dossiers() {
        String adminToken = tokenFor("+237600000003");

        webTestClient.get().uri("/api/v1/admin/kyc/pending")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Object.class)
                .hasSize(3);
    }

    @Test
    void a_bureau_actor_cannot_reach_the_kyc_dashboard() {
        String bureauToken = tokenFor("+237600000001");

        webTestClient.get().uri("/api/v1/admin/kyc/pending")
                .header("Authorization", "Bearer " + bureauToken)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void an_admin_can_validate_a_dossier_and_the_action_is_journalized() {
        String adminToken = tokenFor("+237600000003");
        String idempotencyKey = UUID.randomUUID().toString();
        when(admPort.enregistrerAudit(any(), any(), any(), any(), any())).thenReturn(Mono.empty());

        webTestClient.post().uri("/api/v1/admin/kyc/kyc-1/decision")
                .header("Authorization", "Bearer " + adminToken)
                .header("X-Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"decision\": \"VALIDE\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.statut").isEqualTo("VALIDE");

        verify(admPort).enregistrerAudit(any(), any(), org.mockito.ArgumentMatchers.eq("KYC_DECISION_VALIDE"),
                org.mockito.ArgumentMatchers.eq("kyc-dossier:kyc-1"), any());
    }

    @Test
    void deciding_without_an_idempotency_key_is_rejected() {
        String adminToken = tokenFor("+237600000003");

        webTestClient.post().uri("/api/v1/admin/kyc/kyc-2/decision")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"decision\": \"REJETE\"}")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void deciding_an_unknown_dossier_returns_404() {
        String adminToken = tokenFor("+237600000003");

        webTestClient.post().uri("/api/v1/admin/kyc/kyc-inconnu/decision")
                .header("Authorization", "Bearer " + adminToken)
                .header("X-Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"decision\": \"VALIDE\"}")
                .exchange()
                .expectStatus().isNotFound();
    }
}
