package com.fretcorridor.gateway.infrastructure.rest.cap;

import com.fretcorridor.gateway.domain.cap.PropositionCap;
import com.fretcorridor.gateway.domain.cap.PropositionCapPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UC-MAT-02/diffusion-course : PropositionCapPort mocké, le comportement
 * HTTP réel vers service-cap est couvert séparément (même principe que
 * CapaciteDeclarationControllerTest/RealCapaciteDeclarationAdapterTest).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
class PropositionCapControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private PropositionCapPort propositionCapPort;

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
    void an_authenticated_actor_can_list_their_propositions() {
        String token = tokenFor("+237600000002");
        UUID affectationId = UUID.randomUUID();
        when(propositionCapPort.mesPropositions(any())).thenReturn(Flux.just(new PropositionCap(
                affectationId, UUID.randomUUID(), UUID.randomUUID(), "PROPOSEE",
                "Douala", "Yaoundé", 243000.0, 14400.0, BigDecimal.valueOf(26500),
                Instant.now().plus(15, ChronoUnit.MINUTES), Instant.now())));

        webTestClient.get().uri("/api/v1/transporteur/propositions")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].affectationId").isEqualTo(affectationId.toString())
                .jsonPath("$[0].origineNom").isEqualTo("Douala");
    }

    @Test
    void an_unauthenticated_request_is_rejected() {
        webTestClient.get().uri("/api/v1/transporteur/propositions")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void accepting_a_proposition_relays_the_ids_to_the_port() {
        String token = tokenFor("+237600000002");
        UUID affectationId = UUID.randomUUID();
        UUID demandeId = UUID.randomUUID();
        UUID capaciteId = UUID.randomUUID();
        when(propositionCapPort.accepter(any(), any(), any(), any())).thenReturn(Mono.empty());

        webTestClient.post().uri("/api/v1/transporteur/propositions/{id}/accepter", affectationId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("demandeId", demandeId, "capaciteId", capaciteId))
                .exchange()
                .expectStatus().isNoContent();

        verify(propositionCapPort).accepter(affectationId, demandeId, capaciteId, "mock-ida-delegation-token");
    }

    @Test
    void refusing_a_proposition_relays_the_ids_to_the_port() {
        String token = tokenFor("+237600000002");
        UUID affectationId = UUID.randomUUID();
        UUID demandeId = UUID.randomUUID();
        UUID capaciteId = UUID.randomUUID();
        when(propositionCapPort.refuser(any(), any(), any(), any())).thenReturn(Mono.empty());

        webTestClient.post().uri("/api/v1/transporteur/propositions/{id}/refuser", affectationId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("demandeId", demandeId, "capaciteId", capaciteId))
                .exchange()
                .expectStatus().isNoContent();

        verify(propositionCapPort).refuser(affectationId, demandeId, capaciteId, "mock-ida-delegation-token");
    }
}
