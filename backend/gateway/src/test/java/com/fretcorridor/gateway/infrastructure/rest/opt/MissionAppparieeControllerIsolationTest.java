package com.fretcorridor.gateway.infrastructure.rest.opt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

/** ENF-MUL-01 : un Bureau A ne doit jamais voir les missions du tenant du Bureau B. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
class MissionAppparieeControllerIsolationTest {

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
    void bureau_douala_sees_only_its_own_tenant_missions() {
        String token = tokenFor("+237600000001");

        webTestClient.get().uri("/api/v1/bureau/missions-appariees")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Object.class)
                .hasSize(2);
    }

    @Test
    void bureau_tchad_sees_only_its_own_tenant_mission() {
        String token = tokenFor("+235600000004");

        webTestClient.get().uri("/api/v1/bureau/missions-appariees")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].transporteurNom").isEqualTo("Transporteur Sahel")
                .jsonPath("$[1].transporteurNom").isEqualTo("Logistique Sahel Tchad");
    }

    @Test
    void a_transporteur_cannot_reach_the_bureau_missions_endpoint() {
        String token = tokenFor("+237600000002");

        webTestClient.get().uri("/api/v1/bureau/missions-appariees")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden();
    }
}
