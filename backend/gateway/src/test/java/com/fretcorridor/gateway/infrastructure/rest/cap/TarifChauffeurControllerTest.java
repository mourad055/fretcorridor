package com.fretcorridor.gateway.infrastructure.rest.cap;

import com.fretcorridor.gateway.domain.cap.IntervalleTarif;
import com.fretcorridor.gateway.domain.cap.TarifChauffeur;
import com.fretcorridor.gateway.domain.cap.TarifChauffeurIntrouvableException;
import com.fretcorridor.gateway.domain.cap.TarifChauffeurPort;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Plan §1bis (PR #145) : "mes tarifs" ouvert à tout acteur authentifié
 * (même principe que CapaciteDeclarationController) ; /intervalle ouvert
 * sans JWT côté app Client (écran Estimation). TarifChauffeurPort est
 * mocké : le comportement HTTP réel est couvert par RealTarifChauffeurAdapterTest.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
class TarifChauffeurControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private TarifChauffeurPort tarifChauffeurPort;

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

    private TarifChauffeur tarif() {
        return new TarifChauffeur("tarif-1", "22222222-2222-2222-2222-222222222222", "STANDARD",
                BigDecimal.valueOf(70), true, Instant.now(), Instant.now());
    }

    @Test
    void an_authenticated_actor_can_configure_a_tarif() {
        String token = tokenFor("+237600000002");
        when(tarifChauffeurPort.configurer(any(), any())).thenReturn(Mono.just(tarif()));

        webTestClient.post().uri("/api/v1/mes-tarifs")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"axeId":"22222222-2222-2222-2222-222222222222","categorieMarchandise":"STANDARD","tarifFcfaParKg":70}
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo("tarif-1");
    }

    @Test
    void an_unauthenticated_request_is_rejected() {
        webTestClient.get().uri("/api/v1/mes-tarifs")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void lists_my_tarifs() {
        String token = tokenFor("+237600000002");
        when(tarifChauffeurPort.mesTarifs(any())).thenReturn(Flux.just(tarif()));

        webTestClient.get().uri("/api/v1/mes-tarifs")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Object.class).hasSize(1);
    }

    @Test
    void a_modification_of_an_unknown_tarif_is_reported_as_not_found() {
        String token = tokenFor("+237600000002");
        when(tarifChauffeurPort.modifier(any(), any(), any()))
                .thenReturn(Mono.error(new TarifChauffeurIntrouvableException("Tarif introuvable : tarif-x")));

        webTestClient.patch().uri("/api/v1/mes-tarifs/tarif-x")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"tarifFcfaParKg\": 80}")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.detail").isEqualTo("Tarif introuvable : tarif-x");
    }

    @Test
    void any_authenticated_role_can_read_the_interval() {
        // L'intervalle reste derriere anyExchange().authenticated() (meme principe
        // que /api/v1/axes) : un client final authentifie peut l'appeler, aucun
        // role specifique requis.
        String token = tokenFor("+237600000002");
        when(tarifChauffeurPort.intervalle(any(), any()))
                .thenReturn(Mono.just(new IntervalleTarif(BigDecimal.valueOf(70), BigDecimal.valueOf(100), 3)));

        webTestClient.get().uri("/api/v1/tarifs/intervalle?axeId=22222222-2222-2222-2222-222222222222&categorie=STANDARD")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.nombreChauffeurs").isEqualTo(3);
    }

    @Test
    void an_unknown_interval_is_reported_as_not_found() {
        String token = tokenFor("+237600000002");
        when(tarifChauffeurPort.intervalle(any(), any()))
                .thenReturn(Mono.error(new TarifChauffeurIntrouvableException("aucun chauffeur actif")));

        webTestClient.get().uri("/api/v1/tarifs/intervalle?axeId=axe-1&categorie=STANDARD")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNotFound();
    }
}
