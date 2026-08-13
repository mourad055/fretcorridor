package com.fretcorridor.gateway.infrastructure.rest.flt;

import com.fretcorridor.gateway.domain.flt.PositionPort;
import com.fretcorridor.gateway.domain.flt.PositionRefuseeException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
class EnvoiPositionControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private PositionPort positionPort;

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
    void an_authenticated_actor_can_send_a_position() {
        String token = tokenFor("+237600000002");
        when(positionPort.envoyer(eq("mock-ida-delegation-token"), any())).thenReturn(Mono.empty());

        webTestClient.post().uri("/api/v1/positions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"missionId":"mission-1","latitude":4.05,"longitude":9.7,"horodatage":"2026-08-12T10:00:00"}
                        """)
                .exchange()
                .expectStatus().isCreated();

        verify(positionPort).envoyer(eq("mock-ida-delegation-token"), any());
    }

    @Test
    void an_unauthenticated_request_is_rejected() {
        webTestClient.post().uri("/api/v1/positions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"missionId":"mission-1","latitude":4.05,"longitude":9.7,"horodatage":"2026-08-12T10:00:00"}
                        """)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void a_refusal_from_service_flt_is_reported_as_bad_request() {
        String token = tokenFor("+237600000002");
        when(positionPort.envoyer(any(), any())).thenReturn(Mono.error(new PositionRefuseeException("MISSION_INTROUVABLE")));

        webTestClient.post().uri("/api/v1/positions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"missionId":"mission-inconnue","latitude":4.05,"longitude":9.7,"horodatage":"2026-08-12T10:00:00"}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.detail").isEqualTo("MISSION_INTROUVABLE");
    }
}
