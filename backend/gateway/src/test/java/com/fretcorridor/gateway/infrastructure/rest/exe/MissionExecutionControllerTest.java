package com.fretcorridor.gateway.infrastructure.rest.exe;

import com.fretcorridor.gateway.domain.exe.EtapeExecution;
import com.fretcorridor.gateway.domain.exe.EtapeRefuseeException;
import com.fretcorridor.gateway.domain.exe.MissionExecution;
import com.fretcorridor.gateway.domain.exe.MissionExecutionDetail;
import com.fretcorridor.gateway.domain.exe.MissionExecutionPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * S7 : ouvert à tout acteur authentifié — service-exe applique lui-même
 * l'isolation par transporteurId (PRD §5.3). MissionExecutionPort est mocké :
 * le comportement HTTP réel est couvert par RealMissionExecutionAdapterTest.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
class MissionExecutionControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private MissionExecutionPort missionExecutionPort;

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
    void an_authenticated_actor_lists_their_missions() {
        String token = tokenFor("+237600000002");
        when(missionExecutionPort.mesMissions("mock-ida-delegation-token"))
                .thenReturn(Flux.just(new MissionExecution("mission-1", "EN_ATTENTE", "Douala", "Yaoundé", "2026-08-12T10:00:00")));

        webTestClient.get().uri("/api/v1/missions/mes")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].missionId").isEqualTo("mission-1");
    }

    @Test
    void an_unauthenticated_request_is_rejected() {
        webTestClient.get().uri("/api/v1/missions/mes")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void adding_a_stage_forwards_the_delegation_token() {
        String token = tokenFor("+237600000002");
        when(missionExecutionPort.ajouterEtape(eq("mock-ida-delegation-token"), eq("mission-1"), eq("PRISE_EN_CHARGE"),
                eq("Prise en charge"), any()))
                .thenReturn(Mono.just(new MissionExecutionDetail("mission-1", "PRISE_EN_CHARGE",
                        List.of(new EtapeExecution("PRISE_EN_CHARGE", "Prise en charge", "2026-08-12T10:00:00", "2026-08-12T10:00:05")))));

        webTestClient.post().uri("/api/v1/missions/mission-1/etapes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"type\": \"PRISE_EN_CHARGE\", \"libelle\": \"Prise en charge\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.statut").isEqualTo("PRISE_EN_CHARGE");

        verify(missionExecutionPort).ajouterEtape(eq("mock-ida-delegation-token"), eq("mission-1"), eq("PRISE_EN_CHARGE"),
                eq("Prise en charge"), any());
    }

    @Test
    void a_refusal_from_service_exe_is_reported_as_bad_request() {
        String token = tokenFor("+237600000002");
        when(missionExecutionPort.ajouterEtape(any(), any(), any(), any(), any()))
                .thenReturn(Mono.error(new EtapeRefuseeException("TYPE_ETAPE_INVALIDE")));

        webTestClient.post().uri("/api/v1/missions/mission-1/etapes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"type\": \"INVALIDE\", \"libelle\": \"x\"}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.detail").isEqualTo("TYPE_ETAPE_INVALIDE");
    }
}
