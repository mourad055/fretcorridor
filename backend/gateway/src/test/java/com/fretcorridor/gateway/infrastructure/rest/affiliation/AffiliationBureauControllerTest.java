package com.fretcorridor.gateway.infrastructure.rest.affiliation;

import com.fretcorridor.gateway.domain.affiliation.AffiliationPort;
import com.fretcorridor.gateway.domain.affiliation.AffiliationRefuseeException;
import com.fretcorridor.gateway.domain.affiliation.AffiliationServiceIndisponibleException;
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

/**
 * S18 (audit UX 2026-08-24) : jusqu'ici aucun test ne couvrait ce
 * controller — ni le chemin heureux, ni le mapping d'erreurs (invitation
 * refusée -> 400, pas 500 opaque), ni l'isolation par rôle.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
class AffiliationBureauControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private AffiliationPort affiliationPort;

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
    void a_bureau_actor_invites_a_transporter() {
        String token = tokenFor("+237600000001");
        when(affiliationPort.inviter(any(), eq("+237690000001"))).thenReturn(Mono.empty());

        webTestClient.post().uri("/api/v1/bureau/affiliations")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"telephone\": \"+237690000001\"}")
                .exchange()
                .expectStatus().isCreated();

        verify(affiliationPort).inviter(any(), eq("+237690000001"));
    }

    @Test
    void a_refused_invitation_surfaces_as_a_400_with_the_business_reason_not_a_500() {
        String token = tokenFor("+237600000001");
        when(affiliationPort.inviter(any(), eq("+237699999999")))
                .thenReturn(Mono.error(new AffiliationRefuseeException("ACTEUR_INTROUVABLE")));

        webTestClient.post().uri("/api/v1/bureau/affiliations")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"telephone\": \"+237699999999\"}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.detail").isEqualTo("ACTEUR_INTROUVABLE");
    }

    @Test
    void a_service_unavailable_error_surfaces_as_503() {
        String token = tokenFor("+237600000001");
        when(affiliationPort.inviter(any(), eq("+237690000001")))
                .thenReturn(Mono.error(new AffiliationServiceIndisponibleException()));

        webTestClient.post().uri("/api/v1/bureau/affiliations")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"telephone\": \"+237690000001\"}")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void a_transporteur_actor_cannot_reach_the_bureau_affiliation_endpoint() {
        String token = tokenFor("+237600000002");

        webTestClient.post().uri("/api/v1/bureau/affiliations")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"telephone\": \"+237600000005\"}")
                .exchange()
                .expectStatus().isForbidden();
    }
}
