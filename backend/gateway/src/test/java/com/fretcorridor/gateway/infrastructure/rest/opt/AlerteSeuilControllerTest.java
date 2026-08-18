package com.fretcorridor.gateway.infrastructure.rest.opt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

/** EF-BUR-07 (S) : configuration d'alertes sur seuils par l'agent Bureau. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
class AlerteSeuilControllerTest {

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
    void configures_lists_and_deletes_an_alerte() {
        String token = tokenFor("+237600000001");

        byte[] corps = webTestClient.post().uri("/api/v1/bureau/alertes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("axeId", "axe-1", "indicateur", "PRIX_MEDIANE", "comparateur", "SUPERIEUR", "seuil", 25000))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.axeId").isEqualTo("axe-1")
                .jsonPath("$.indicateur").isEqualTo("PRIX_MEDIANE")
                .jsonPath("$.creeParActeurId").isEqualTo("actor-bureau-1")
                .returnResult()
                .getResponseBody();
        String alerteId = new String(corps).replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        webTestClient.get().uri("/api/v1/bureau/alertes")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[?(@.id=='" + alerteId + "')]").exists();

        webTestClient.delete().uri("/api/v1/bureau/alertes/{id}", alerteId)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNoContent();

        webTestClient.get().uri("/api/v1/bureau/alertes")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[?(@.id=='" + alerteId + "')]").doesNotExist();
    }

    @Test
    void a_transporteur_cannot_reach_the_bureau_alertes_endpoint() {
        String token = tokenFor("+237600000002");

        webTestClient.get().uri("/api/v1/bureau/alertes")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden();
    }
}
