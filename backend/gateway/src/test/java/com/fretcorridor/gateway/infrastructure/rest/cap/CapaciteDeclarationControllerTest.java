package com.fretcorridor.gateway.infrastructure.rest.cap;

import com.fretcorridor.gateway.domain.cap.CapaciteDeclaree;
import com.fretcorridor.gateway.domain.cap.CapaciteDeclarationPort;
import com.fretcorridor.gateway.domain.cap.CapaciteRefuseeException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * S4 (EF-CAP-03) : ouvert à tout acteur authentifié (pas de restriction de
 * rôle spécifique — chauffeur, transporteur et chauffeur-propriétaire
 * peuvent tous déclarer une capacité). CapaciteDeclarationPort est mocké :
 * le comportement HTTP réel est couvert par RealCapaciteDeclarationAdapterTest.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
class CapaciteDeclarationControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CapaciteDeclarationPort capaciteDeclarationPort;

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

    private String corpsRequete() {
        return """
                {
                  "vehiculeId": "11111111-1111-1111-1111-111111111111",
                  "axeId": "22222222-2222-2222-2222-222222222222",
                  "modeDeclaration": "TOTALE",
                  "poidsKg": 9500,
                  "origineLatitude": 4.05,
                  "origineLongitude": 9.7,
                  "typeVehicule": "Camion 10T",
                  "profilMatieresDangereuses": false,
                  "dateDepart": "%s"
                }
                """.formatted(Instant.now().plus(1, ChronoUnit.DAYS));
    }

    @Test
    void an_authenticated_actor_can_declare_a_capacity() {
        String token = tokenFor("+237600000002");
        when(capaciteDeclarationPort.declarer(any())).thenReturn(Mono.just(new CapaciteDeclaree(
                "cap-1", "11111111-1111-1111-1111-111111111111", "22222222-2222-2222-2222-222222222222",
                "TOTALE", BigDecimal.valueOf(9500), BigDecimal.valueOf(9500), BigDecimal.valueOf(9500),
                false, true, Instant.now().plus(1, ChronoUnit.DAYS), Instant.now())));

        webTestClient.post().uri("/api/v1/capacites")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(corpsRequete())
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.publiee").isEqualTo(true);
    }

    @Test
    void an_unauthenticated_request_is_rejected() {
        webTestClient.post().uri("/api/v1/capacites")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(corpsRequete())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void missing_required_fields_are_rejected_before_reaching_service_cap() {
        String token = tokenFor("+237600000002");

        webTestClient.post().uri("/api/v1/capacites")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void a_refusal_from_service_cap_is_reported_as_bad_request() {
        String token = tokenFor("+237600000002");
        when(capaciteDeclarationPort.declarer(any()))
                .thenReturn(Mono.error(new CapaciteRefuseeException("Validation échouée")));

        webTestClient.post().uri("/api/v1/capacites")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(corpsRequete())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.detail").isEqualTo("Validation échouée");
    }
}
