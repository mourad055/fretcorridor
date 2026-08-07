package com.fretcorridor.gateway.infrastructure.rest.exe;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

/**
 * PRD §9 S7 : un Transporteur ne voit que ses missions ; un Bureau voit
 * celles de son territoire.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
class MissionChronologieControllerIsolationTest {

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
    void bureau_douala_sees_both_missions_of_its_territory() {
        String token = tokenFor("+237600000001");

        webTestClient.get().uri("/api/v1/bureau/missions-chronologie")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Object.class)
                .hasSize(2);
    }

    @Test
    void bureau_tchad_sees_only_its_own_territory_missions() {
        String token = tokenFor("+235600000004");

        webTestClient.get().uri("/api/v1/bureau/missions-chronologie")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Object.class)
                .hasSize(2);
    }

    @Test
    void transporteur_un_sees_only_its_own_mission_never_transporteur_deux_s() {
        String token = tokenFor("+237600000002");

        webTestClient.get().uri("/api/v1/transporteur/missions")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].id").isEqualTo("mission-a");
    }

    @Test
    void transporteur_deux_sees_only_its_own_mission() {
        String token = tokenFor("+237600000005");

        webTestClient.get().uri("/api/v1/transporteur/missions")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].id").isEqualTo("mission-b");
    }

    @Test
    void an_admin_cannot_reach_the_transporteur_missions_endpoint() {
        String token = tokenFor("+237600000003");

        webTestClient.get().uri("/api/v1/transporteur/missions")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden();
    }
}
