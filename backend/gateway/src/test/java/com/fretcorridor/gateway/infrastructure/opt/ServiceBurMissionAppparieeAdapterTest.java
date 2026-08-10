package com.fretcorridor.gateway.infrastructure.opt;

import com.fretcorridor.gateway.domain.opt.MissionAppariee;
import com.fretcorridor.gateway.domain.opt.StatutMission;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceBurMissionAppparieeAdapterTest {

    private MockWebServer serviceBur;
    private ServiceBurMissionAppparieeAdapter adapter;

    @BeforeEach
    void demarrerServiceBurFactice() throws IOException {
        serviceBur = new MockWebServer();
        serviceBur.start();
        adapter = new ServiceBurMissionAppparieeAdapter(WebClient.builder(), serviceBur.url("/").toString());
    }

    @AfterEach
    void arreterServiceBurFactice() throws IOException {
        serviceBur.shutdown();
    }

    @Test
    void maps_service_bur_missions_always_as_confirmee_with_transporteur_id_as_a_placeholder_name() {
        serviceBur.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        [{"missionId":"mission-1","axeId":"axe-1","transporteurId":"transp-1","origineNom":"Douala","destinationNom":"Yaoundé","prixTransport":50000,"devise":"XAF","confirmeeLe":"2026-08-10T12:00:00Z"}]
                        """));

        StepVerifier.create(adapter.listerMissionsParTenant("tenant-bgft-douala").collectList())
                .assertNext(missions -> {
                    assertThat(missions).hasSize(1);
                    MissionAppariee mission = missions.get(0);
                    assertThat(mission.id()).isEqualTo("mission-1");
                    assertThat(mission.tenantId()).isEqualTo("tenant-bgft-douala");
                    assertThat(mission.axeId()).isEqualTo("axe-1");
                    assertThat(mission.transporteurNom()).isEqualTo("transp-1");
                    assertThat(mission.origine()).isEqualTo("Douala");
                    assertThat(mission.destination()).isEqualTo("Yaoundé");
                    assertThat(mission.statut()).isEqualTo(StatutMission.CONFIRMEE);
                })
                .verifyComplete();
    }

    @Test
    void sends_the_tenant_id_as_a_query_param() throws InterruptedException {
        serviceBur.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("[]"));

        adapter.listerMissionsParTenant("tenant-bnft-ndjamena").collectList().block();

        var requete = serviceBur.takeRequest();
        assertThat(requete.getPath()).isEqualTo("/api/v1/bur/missions-appariees?tenantId=tenant-bnft-ndjamena");
    }
}
