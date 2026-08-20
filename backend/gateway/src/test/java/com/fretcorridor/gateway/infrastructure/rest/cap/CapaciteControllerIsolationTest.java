package com.fretcorridor.gateway.infrastructure.rest.cap;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

/**
 * PRD §5.3 : périmètre strict par acteur — un Transporteur ne voit jamais la
 * capacité d'un autre transporteur, même au sein du même tenant.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
class CapaciteControllerIsolationTest {

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
    void transporteur_un_sees_only_its_own_capacities() {
        String token = tokenFor("+237600000002");

        webTestClient.get().uri("/api/v1/transporteur/capacites")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Object.class)
                .hasSize(2);
    }

    @Test
    void transporteur_deux_sees_only_its_own_capacity_never_transporteur_un_s() {
        String token = tokenFor("+237600000005");

        webTestClient.get().uri("/api/v1/transporteur/capacites")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].vehicule").isEqualTo("Fourgon 3T — LT 5678 CD");
    }

    @Test
    void a_bureau_actor_cannot_reach_the_transporteur_capacites_endpoint() {
        String token = tokenFor("+237600000001");

        webTestClient.get().uri("/api/v1/transporteur/capacites")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden();
    }
}
