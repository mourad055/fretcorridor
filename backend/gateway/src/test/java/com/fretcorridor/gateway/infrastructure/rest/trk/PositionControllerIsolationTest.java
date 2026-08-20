package com.fretcorridor.gateway.infrastructure.rest.trk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

/**
 * ENF-MUL-01 : un Bureau A ne doit jamais voir les positions du tenant du
 * Bureau B. RG-043 : toute position restituée porte un âge exploitable.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
class PositionControllerIsolationTest {

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
    void bureau_douala_sees_only_its_own_tenant_positions_with_age() {
        String token = tokenFor("+237600000001");

        webTestClient.get().uri("/api/v1/bureau/positions")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].ageSecondes").exists()
                .jsonPath("$[0].ageSecondes").isNumber()
                .jsonPath("$[1].ageSecondes").exists();
    }

    @Test
    void bureau_tchad_sees_only_its_own_tenant_position() {
        String token = tokenFor("+235600000004");

        webTestClient.get().uri("/api/v1/bureau/positions")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].vehiculeLabel").isEqualTo("Camion 8T — TD 4321 EF")
                .jsonPath("$[0].ageSecondes").exists()
                .jsonPath("$[1].vehiculeLabel").isEqualTo("Camion 10T — TD 9012 GH")
                .jsonPath("$[1].ageSecondes").exists();
    }

    @Test
    void a_transporteur_cannot_reach_the_bureau_positions_endpoint() {
        String token = tokenFor("+237600000002");

        webTestClient.get().uri("/api/v1/bureau/positions")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden();
    }
}
