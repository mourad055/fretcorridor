package com.fretcorridor.gateway.infrastructure.rest.not;

import com.fretcorridor.gateway.domain.not.CanalNotification;
import com.fretcorridor.gateway.domain.not.Notification;
import com.fretcorridor.gateway.domain.not.NotificationPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/** PRD §9 S9 : un Bureau ne voit que les notifications de son propre tenant (ENF-MUL-01). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
class NotificationControllerIsolationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private NotificationPort notificationPort;

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
        when(notificationPort.listerNotificationsParTenant(eq("tenant-bgft-douala"), any()))
                .thenReturn(Flux.just(
                        new Notification("not-1", "tenant-bgft-douala", CanalNotification.EMAIL,
                                "bureau.douala@bgft.example", "Nouvelle mission appariée", "Resume 1", Instant.now()),
                        new Notification("not-2", "tenant-bgft-douala", CanalNotification.EMAIL,
                                "bureau.douala@bgft.example", "Écart de réconciliation détecté", "Resume 2", Instant.now())
                ));

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
        when(notificationPort.listerNotificationsParTenant(eq("tenant-bnft-ndjamena"), any()))
                .thenReturn(Flux.just(
                        new Notification("not-3", "tenant-bnft-ndjamena", CanalNotification.EMAIL,
                                "bureau.tchad@bnft.example", "Dossier KYC validé", "Resume 3", Instant.now()),
                        new Notification("not-4", "tenant-bnft-ndjamena", CanalNotification.EMAIL,
                                "bureau.tchad@bnft.example", "Nouvelle mission appariée", "Resume 4", Instant.now())
                ));

        webTestClient.get().uri("/api/v1/bureau/notifications")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].id").isEqualTo("not-3")
                .jsonPath("$[1].id").isEqualTo("not-4");
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
