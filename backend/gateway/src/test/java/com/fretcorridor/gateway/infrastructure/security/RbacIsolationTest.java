package com.fretcorridor.gateway.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * ENF-SEC-04 (échelle Sprint 1) : tente activement des accès transverses depuis
 * chaque rôle sur les endpoints protégés de la gateway. Un échec de cette suite
 * doit bloquer la livraison.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
class RbacIsolationTest {

    @Autowired
    private WebTestClient webTestClient;

    private String tokenFor(String phone) {
        return webTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"phone\": \"" + phone + "\", \"code\": \"123456\"}")
                .exchange()
                .expectStatus().isOk()
                .returnResult(java.util.Map.class)
                .getResponseBody()
                .blockFirst()
                .get("token")
                .toString();
    }

    @Test
    void bureau_actor_can_reach_bureau_endpoint_only() {
        String bureauToken = tokenFor("+237600000001");

        webTestClient.get().uri("/api/v1/bureau/ping")
                .header("Authorization", "Bearer " + bureauToken)
                .exchange().expectStatus().isOk();

        webTestClient.get().uri("/api/v1/transporteur/ping")
                .header("Authorization", "Bearer " + bureauToken)
                .exchange().expectStatus().isForbidden();

        webTestClient.get().uri("/api/v1/admin/ping")
                .header("Authorization", "Bearer " + bureauToken)
                .exchange().expectStatus().isForbidden();
    }

    @Test
    void transporteur_actor_can_reach_transporteur_endpoint_only() {
        String transporteurToken = tokenFor("+237600000002");

        webTestClient.get().uri("/api/v1/transporteur/ping")
                .header("Authorization", "Bearer " + transporteurToken)
                .exchange().expectStatus().isOk();

        webTestClient.get().uri("/api/v1/bureau/ping")
                .header("Authorization", "Bearer " + transporteurToken)
                .exchange().expectStatus().isForbidden();

        webTestClient.get().uri("/api/v1/admin/ping")
                .header("Authorization", "Bearer " + transporteurToken)
                .exchange().expectStatus().isForbidden();
    }

    @Test
    void admin_actor_can_reach_admin_endpoint_only() {
        String adminToken = tokenFor("+237600000003");

        webTestClient.get().uri("/api/v1/admin/ping")
                .header("Authorization", "Bearer " + adminToken)
                .exchange().expectStatus().isOk();

        webTestClient.get().uri("/api/v1/bureau/ping")
                .header("Authorization", "Bearer " + adminToken)
                .exchange().expectStatus().isForbidden();

        webTestClient.get().uri("/api/v1/transporteur/ping")
                .header("Authorization", "Bearer " + adminToken)
                .exchange().expectStatus().isForbidden();
    }

    @Test
    void unauthenticated_request_is_rejected() {
        webTestClient.get().uri("/api/v1/bureau/ping")
                .exchange().expectStatus().isUnauthorized();
    }
}
