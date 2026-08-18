package com.fretcorridor.gateway.infrastructure.exe;

import com.fretcorridor.gateway.domain.exe.EtapeRefuseeException;
import com.fretcorridor.gateway.domain.exe.ExeServiceIndisponibleException;
import com.fretcorridor.gateway.domain.exe.MissionIntrouvableException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class RealMissionExecutionAdapterTest {

    private MockWebServer serviceExe;
    private RealMissionExecutionAdapter adapter;

    @BeforeEach
    void demarrerServiceExeFactice() throws IOException {
        serviceExe = new MockWebServer();
        serviceExe.start();
        adapter = new RealMissionExecutionAdapter(WebClient.builder(), serviceExe.url("/").toString());
    }

    @AfterEach
    void arreterServiceExeFactice() throws IOException {
        serviceExe.shutdown();
    }

    @Test
    void lists_my_missions_with_the_delegation_token() throws InterruptedException {
        serviceExe.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        [{"missionId":"mission-1","statut":"EN_ATTENTE","origineNom":"Douala","destinationNom":"Yaoundé","dateCreation":"2026-08-12T10:00:00"}]
                        """));

        StepVerifier.create(adapter.mesMissions("delegation-token-1"))
                .expectNextMatches(m -> m.missionId().equals("mission-1") && m.statut().equals("EN_ATTENTE"))
                .verifyComplete();

        var requete = serviceExe.takeRequest();
        assertThat(requete.getPath()).isEqualTo("/api/missions/mes");
        assertThat(requete.getHeader("Authorization")).isEqualTo("Bearer delegation-token-1");
    }

    @Test
    void adds_a_stage_and_returns_the_updated_chronology() throws InterruptedException {
        serviceExe.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"missionId":"mission-1","statut":"PRISE_EN_CHARGE","etapes":[
                          {"type":"PRISE_EN_CHARGE","libelle":"Prise en charge","horodatageCapture":"2026-08-12T10:00:00","horodatageTransmission":"2026-08-12T10:00:05"}
                        ]}
                        """));

        StepVerifier.create(adapter.ajouterEtape("delegation-token-1", "mission-1", "PRISE_EN_CHARGE", "Prise en charge", null))
                .expectNextMatches(d -> d.statut().equals("PRISE_EN_CHARGE") && d.etapes().size() == 1)
                .verifyComplete();

        var requete = serviceExe.takeRequest();
        assertThat(requete.getPath()).isEqualTo("/api/missions/mission-1/etapes");
        assertThat(requete.getHeader("Authorization")).isEqualTo("Bearer delegation-token-1");
    }

    @Test
    void maps_a_404_to_a_mission_not_found_error() {
        serviceExe.enqueue(new MockResponse().setResponseCode(404));

        StepVerifier.create(adapter.chronologie("delegation-token-1", "mission-inconnue"))
                .expectError(MissionIntrouvableException.class)
                .verify();
    }

    @Test
    void maps_a_400_to_an_etape_refused_error() {
        serviceExe.enqueue(new MockResponse().setResponseCode(400).setBody("TYPE_ETAPE_INVALIDE"));

        StepVerifier.create(adapter.ajouterEtape("delegation-token-1", "mission-1", "INVALIDE", "x", null))
                .expectError(EtapeRefuseeException.class)
                .verify();
    }

    @Test
    void refuses_to_call_service_exe_without_a_delegation_token() {
        StepVerifier.create(adapter.mesMissions(null))
                .expectError(ExeServiceIndisponibleException.class)
                .verify();

        assertThat(serviceExe.getRequestCount()).isZero();
    }
}
