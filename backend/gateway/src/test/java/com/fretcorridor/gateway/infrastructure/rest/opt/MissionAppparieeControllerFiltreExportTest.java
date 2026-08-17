package com.fretcorridor.gateway.infrastructure.rest.opt;

import com.fretcorridor.gateway.domain.adm.AdmPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * EF-BUR-02 : filtrage, détail et export des flux supervisés par le Bureau.
 * Complète {@link MissionAppparieeControllerIsolationTest}, dédié à ENF-MUL-01.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
class MissionAppparieeControllerFiltreExportTest {

    @Autowired
    private WebTestClient webTestClient;

    /** EF-BUR-06 : le détail d'une mission journalise sa consultation via AdmPort — pas de service-adm réel ici. */
    @MockBean
    private AdmPort admPort;

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
    void filters_by_statut() {
        String token = tokenFor("+237600000001");

        webTestClient.get().uri("/api/v1/bureau/missions-appariees?statut=EN_COURS")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].statut").isEqualTo("EN_COURS");
    }

    @Test
    void filters_by_axe_id() {
        String token = tokenFor("+237600000001");

        webTestClient.get().uri("/api/v1/bureau/missions-appariees?axeId=axe-1")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].axeId").isEqualTo("axe-1");
    }

    @Test
    void returns_detail_of_a_mission_in_the_actor_tenant() {
        when(admPort.enregistrerAudit(any(), any(), any(), any())).thenReturn(Mono.empty());
        String token = tokenFor("+237600000001");

        webTestClient.get().uri("/api/v1/bureau/missions-appariees/mission-1")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("mission-1")
                .jsonPath("$.transporteurNom").isEqualTo("Transport Étoile SARL");

        org.mockito.Mockito.verify(admPort)
                .enregistrerAudit("tenant-bgft-douala", "actor-bureau-1", "CONSULTATION_MISSION_DETAIL", "mission:mission-1");
    }

    @Test
    void returns_404_for_a_mission_that_does_not_exist() {
        when(admPort.enregistrerAudit(any(), any(), any(), any())).thenReturn(Mono.empty());
        String token = tokenFor("+237600000001");

        webTestClient.get().uri("/api/v1/bureau/missions-appariees/mission-inconnue")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void returns_404_rather_than_leaking_a_mission_from_another_tenant() {
        when(admPort.enregistrerAudit(any(), any(), any(), any())).thenReturn(Mono.empty());
        String token = tokenFor("+237600000001");

        // mission-3 appartient à tenant-bnft-ndjamena, pas au tenant du token ci-dessus.
        webTestClient.get().uri("/api/v1/bureau/missions-appariees/mission-3")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void exports_the_supervised_flow_as_csv() {
        String token = tokenFor("+237600000001");

        String csv = new String(webTestClient.get().uri("/api/v1/bureau/missions-appariees/export")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("text/csv")
                .expectHeader().valueEquals("Content-Disposition", "attachment; filename=\"missions-supervisees.csv\"")
                .expectBody()
                .returnResult()
                .getResponseBody());

        assertThat(csv)
                .startsWith("id,axeId,transporteurNom,origine,destination,enlevementLe,statut\n")
                .contains("mission-1,axe-1,Transport Étoile SARL,Douala,Yaoundé")
                .doesNotContain("mission-3");
    }

    /** EF-BUR-03 : indicateurs de marché d'un axe — agrégat, jamais journalisé (RG-086). */
    @Test
    void returns_observatoire_indicators_for_an_axe() {
        String token = tokenFor("+237600000001");

        webTestClient.get().uri("/api/v1/bureau/observatoire/axe-1")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.axeId").isEqualTo("axe-1")
                .jsonPath("$.seuilAtteint").isEqualTo(false);

        org.mockito.Mockito.verifyNoInteractions(admPort);
    }

    @Test
    void a_transporteur_cannot_reach_the_bureau_observatoire_endpoint() {
        String token = tokenFor("+237600000002");

        webTestClient.get().uri("/api/v1/bureau/observatoire/axe-1")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden();
    }

    /** EF-BUR-05, RG-087 : un agent Bureau déclare l'estimation de marché d'un axe. */
    @Test
    void a_bureau_agent_can_declare_a_market_estimation_for_an_axe() {
        String token = tokenFor("+237600000001");

        webTestClient.put().uri("/api/v1/bureau/observatoire/axe-1/estimation-marche")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("volumeMensuelEstime", 350, "source", "enquête terrain Q1 2026"))
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void rejects_a_non_positive_market_estimation() {
        String token = tokenFor("+237600000001");

        webTestClient.put().uri("/api/v1/bureau/observatoire/axe-1/estimation-marche")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("volumeMensuelEstime", 0, "source", "enquête terrain Q1 2026"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void a_transporteur_cannot_declare_a_market_estimation() {
        String token = tokenFor("+237600000002");

        webTestClient.put().uri("/api/v1/bureau/observatoire/axe-1/estimation-marche")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("volumeMensuelEstime", 350, "source", "enquête terrain Q1 2026"))
                .exchange()
                .expectStatus().isForbidden();
    }
}
