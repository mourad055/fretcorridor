package com.fretcorridor.gateway.infrastructure.rest.not;

import com.fretcorridor.gateway.domain.not.NotificationMobile;
import com.fretcorridor.gateway.domain.not.NotificationMobilePort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
class NotificationMobileControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private NotificationMobilePort notificationMobilePort;

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
    void an_authenticated_actor_lists_their_notifications() {
        String token = tokenFor("+237600000002");
        when(notificationMobilePort.mesNotifications("mock-ida-delegation-token"))
                .thenReturn(Flux.just(new NotificationMobile("n1", "Nouvelle mission", "Une mission vous est proposée",
                        "MISSION", "mission-1", false, "2026-08-12T10:00:00", null)));

        webTestClient.get().uri("/api/v1/notifications/mes")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].id").isEqualTo("n1")
                .jsonPath("$[0].lue").isEqualTo(false);
    }

    @Test
    void an_unauthenticated_request_is_rejected() {
        webTestClient.get().uri("/api/v1/notifications/mes")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void returns_the_unread_count() {
        String token = tokenFor("+237600000002");
        when(notificationMobilePort.nombreNonLues("mock-ida-delegation-token")).thenReturn(Mono.just(3));

        webTestClient.get().uri("/api/v1/notifications/mes/non-lues")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.nombre").isEqualTo(3);
    }

    @Test
    void marking_a_notification_as_read_forwards_the_delegation_token() {
        String token = tokenFor("+237600000002");
        when(notificationMobilePort.marquerLue("mock-ida-delegation-token", "n1")).thenReturn(Mono.empty());

        webTestClient.patch().uri("/api/v1/notifications/mes/n1/lue")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNoContent();

        verify(notificationMobilePort).marquerLue(eq("mock-ida-delegation-token"), eq("n1"));
    }

    @Test
    void responding_to_a_return_trip_proposal_forwards_the_delegation_token_and_answer() {
        String token = tokenFor("+237600000002");
        when(notificationMobilePort.repondre("mock-ida-delegation-token", "n1", true)).thenReturn(Mono.empty());

        webTestClient.patch().uri("/api/v1/notifications/mes/n1/repondre")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("accepte", true))
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNoContent();

        verify(notificationMobilePort).repondre(eq("mock-ida-delegation-token"), eq("n1"), eq(true));
    }
}
