package com.fretcorridor.gateway.infrastructure.rest.geo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

/**
 * S3 (app Chauffeur/Transporteur, EF-GEO-03) : GET /api/v1/axes est ouvert à
 * tout acteur authentifié (contrairement à /api/v1/bureau/axes, réservé au
 * rôle BUREAU), mais reste isolé par tenant.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
class AxeControllerMobileTest {

    @Autowired
    private WebTestClient webTestClient;

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
    void a_transporteur_sees_only_its_own_tenant_axes_with_locks_visible() {
        String token = tokenFor("+237600000002");

        webTestClient.get().uri("/api/v1/axes")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[1].matchingActif").isEqualTo(true)
                .jsonPath("$[1].paiementActif").isEqualTo(false);
    }

    @Test
    void an_unauthenticated_request_is_rejected() {
        webTestClient.get().uri("/api/v1/axes")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
