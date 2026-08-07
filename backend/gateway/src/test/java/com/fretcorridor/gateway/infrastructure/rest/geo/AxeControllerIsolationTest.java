package com.fretcorridor.gateway.infrastructure.rest.geo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

/**
 * ENF-MUL-01 : un Bureau A ne doit jamais voir les axes du tenant du Bureau B.
 * Critère de sortie explicite du Sprint 3 (PRD §9).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
class AxeControllerIsolationTest {

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
    void bureau_douala_sees_only_its_own_tenant_axes() {
        String token = tokenFor("+237600000001");

        webTestClient.get().uri("/api/v1/bureau/axes")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Object.class)
                .hasSize(2);
    }

    @Test
    void bureau_tchad_sees_only_its_own_tenant_axes_and_none_of_douala() {
        String token = tokenFor("+235600000004");

        webTestClient.get().uri("/api/v1/bureau/axes")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].origine").isEqualTo("N'Djamena")
                .jsonPath("$[0].destination").isEqualTo("Moundou")
                .jsonPath("$[1].origine").isEqualTo("N'Djamena")
                .jsonPath("$[1].destination").isEqualTo("Sarh");
    }

    @Test
    void a_transporteur_cannot_reach_the_bureau_axes_endpoint() {
        String token = tokenFor("+237600000002");

        webTestClient.get().uri("/api/v1/bureau/axes")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden();
    }
}
