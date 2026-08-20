package com.fretcorridor.gateway.infrastructure.rest.adm;

import com.fretcorridor.gateway.domain.adm.AdmPort;
import com.fretcorridor.gateway.domain.adm.DossierVue;
import com.fretcorridor.gateway.domain.exe.EtapeEtat;
import com.fretcorridor.gateway.domain.exe.EtapeMission;
import com.fretcorridor.gateway.domain.exe.EtapeType;
import com.fretcorridor.gateway.domain.exe.ExePort;
import com.fretcorridor.gateway.domain.exe.Mission;
import com.fretcorridor.gateway.domain.pay.EcritureVue;
import com.fretcorridor.gateway.domain.pay.PayReadPort;
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
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** FE-ADM-01/02 (Sprint 10) : file de travail, dossier consolidé, RBAC ADMIN. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
class DossierControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private AdmPort admPort;

    @MockBean
    private ExePort exePort;

    @MockBean
    private PayReadPort payReadPort;

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

    private DossierVue dossier(String missionId) {
        return new DossierVue("dossier-1", "tenant-bgft-douala", "LITIGE", "NORMALE", "OUVERT", missionId,
                List.of("acteur-transporteur-1"), List.of(), Instant.now(), Instant.now().plusSeconds(3600),
                null, null, null, null, null);
    }

    @Test
    void a_bureau_actor_cannot_reach_the_admin_dossiers_endpoint() {
        String token = tokenFor("+237600000001");

        webTestClient.get().uri("/api/v1/admin/dossiers?tenantId=tenant-bgft-douala")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void an_admin_sees_the_file_de_travail_of_a_tenant() {
        String token = tokenFor("+237600000003");
        when(admPort.fileDeTravail(eq("tenant-bgft-douala"), any())).thenReturn(Flux.just(dossier(null)));

        webTestClient.get().uri("/api/v1/admin/dossiers?tenantId=tenant-bgft-douala")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Object.class)
                .hasSize(1);
    }

    @Test
    void consolidated_dossier_with_no_mission_has_no_chronology_nor_ecritures() {
        String token = tokenFor("+237600000003");
        when(admPort.dossier(eq("dossier-1"), any())).thenReturn(Mono.just(dossier(null)));

        webTestClient.get().uri("/api/v1/admin/dossiers/dossier-1")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.mission").value(org.hamcrest.Matchers.nullValue())
                .jsonPath("$.ecritures.length()").isEqualTo(0);
    }

    @Test
    void consolidated_dossier_with_a_mission_aggregates_chronology_and_ecritures() {
        String token = tokenFor("+237600000003");
        when(admPort.dossier(eq("dossier-1"), any())).thenReturn(Mono.just(dossier("mission-a")));
        when(exePort.listerMissionsParTenant("tenant-bgft-douala")).thenReturn(Flux.just(
                new Mission("mission-a", "tenant-bgft-douala", "actor-transporteur-1", "Transport Étoile SARL",
                        "Douala", "Yaoundé", List.of(new EtapeMission(1, EtapeType.ENLEVEMENT, "Douala", EtapeEtat.TERMINEE)))
        ));
        when(payReadPort.rapportDuTenant(eq("tenant-bgft-douala"), any())).thenReturn(Flux.just(
                new EcritureVue("e1", "mission-a", "COMPTE_SEQUESTRE_PRESTATAIRE", "ENCAISSEMENT", "CREDIT",
                        new BigDecimal("500"), Instant.now(), "VALIDE", "VIREMENT", false),
                new EcritureVue("e2", "mission-autre", "COMPTE_SEQUESTRE_PRESTATAIRE", "ENCAISSEMENT", "CREDIT",
                        new BigDecimal("100"), Instant.now(), "VALIDE", "VIREMENT", false)
        ));

        webTestClient.get().uri("/api/v1/admin/dossiers/dossier-1")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.mission.id").isEqualTo("mission-a")
                .jsonPath("$.ecritures.length()").isEqualTo(1)
                .jsonPath("$.ecritures[0].id").isEqualTo("e1");
    }

    @Test
    void prise_en_charge_uses_the_authenticated_actor_id_never_a_client_supplied_one() {
        String token = tokenFor("+237600000003");
        when(admPort.priseEnCharge(eq("dossier-1"), eq("actor-admin-1"), any())).thenReturn(Mono.just(dossier(null)));

        webTestClient.post().uri("/api/v1/admin/dossiers/dossier-1/prise-en-charge")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk();

        verify(admPort).priseEnCharge(eq("dossier-1"), eq("actor-admin-1"), any());
    }

    @Test
    void decision_uses_the_authenticated_actor_id_never_a_client_supplied_one() {
        String token = tokenFor("+237600000003");
        when(admPort.decider(eq("dossier-1"), eq("RESOLU"), eq("motif"), eq("actor-admin-1"), any()))
                .thenReturn(Mono.just(dossier(null)));

        webTestClient.post().uri("/api/v1/admin/dossiers/dossier-1/decision")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"decision\": \"RESOLU\", \"motif\": \"motif\"}")
                .exchange()
                .expectStatus().isOk();

        verify(admPort).decider(eq("dossier-1"), eq("RESOLU"), eq("motif"), eq("actor-admin-1"), any());
    }
}
