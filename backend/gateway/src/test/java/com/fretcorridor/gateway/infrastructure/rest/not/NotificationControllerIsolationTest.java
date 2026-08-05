package com.fretcorridor.gateway.infrastructure.rest.not;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

/** PRD §9 S9 : un Bureau ne voit que les notifications de son propre tenant (ENF-MUL-01). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
class NotificationControllerIsolationTest {

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
    void bureau_douala_sees_its_own_tenant_notifications() {
        String token = tokenFor("+237600000001");

        webTestClient.get().uri("/api/v1/bureau/notifications")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Object.class)
                .hasSize(2);
    }

    @Test
    void bureau_tchad_never_sees_douala_s_notifications() {
        String token = tokenFor("+235600000004");

        webTestClient.get().uri("/api/v1/bureau/notifications")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].id").isEqualTo("not-3");
    }

    @Test
    void a_transporteur_cannot_reach_the_bureau_notifications_endpoint() {
        String token = tokenFor("+237600000002");

        webTestClient.get().uri("/api/v1/bureau/notifications")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden();
    }
}
