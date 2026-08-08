package com.fretcorridor.gateway.infrastructure.rest.opt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EF-BUR-02 : filtrage, détail et export des flux supervisés par le Bureau.
 * Complète {@link MissionAppparieeControllerIsolationTest}, dédié à ENF-MUL-01.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
class MissionAppparieeControllerFiltreExportTest {

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
        String token = tokenFor("+237600000001");

        webTestClient.get().uri("/api/v1/bureau/missions-appariees/mission-1")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("mission-1")
                .jsonPath("$.transporteurNom").isEqualTo("Transport Étoile SARL");
    }

    @Test
    void returns_404_for_a_mission_that_does_not_exist() {
        String token = tokenFor("+237600000001");

        webTestClient.get().uri("/api/v1/bureau/missions-appariees/mission-inconnue")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void returns_404_rather_than_leaking_a_mission_from_another_tenant() {
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
}
