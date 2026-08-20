package com.fretcorridor.gateway.infrastructure.trk;

import com.fretcorridor.gateway.domain.trk.PositionVehicule;
import com.fretcorridor.gateway.domain.trk.TrkServiceIndisponibleException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceBurPositionAdapterTest {

    private static final String MISSION_ID = "3fa85f64-5717-4562-b3fc-2c963f66afa6";
    private static final String VEHICULE_ID = "9c858901-8a57-4791-81fe-4c455b099bc9";

    private MockWebServer serviceBur;
    private ServiceBurPositionAdapter adapter;

    @BeforeEach
    void demarrerServiceBurFactice() throws IOException {
        serviceBur = new MockWebServer();
        serviceBur.start();
        adapter = new ServiceBurPositionAdapter(WebClient.builder(), serviceBur.url("/").toString());
    }

    @AfterEach
    void arreterServiceBurFactice() throws IOException {
        serviceBur.shutdown();
    }

    @Test
    void maps_service_bur_positions_with_vehicule_id_as_a_placeholder_label() {
        serviceBur.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        [{"missionId":"%s","vehiculeId":"%s","latitude":4.05,"longitude":9.76,"capturedLe":"2026-08-10T12:00:00Z"}]
                        """.formatted(MISSION_ID, VEHICULE_ID)));

        StepVerifier.create(adapter.listerPositionsParTenant("tenant-bgft-douala", "delegation-token-1").collectList())
                .assertNext(positions -> {
                    assertThat(positions).hasSize(1);
                    PositionVehicule position = positions.get(0);
                    assertThat(position.id()).isEqualTo(MISSION_ID);
                    assertThat(position.tenantId()).isEqualTo("tenant-bgft-douala");
                    assertThat(position.vehiculeLabel()).isEqualTo(VEHICULE_ID);
                    assertThat(position.latitude()).isEqualTo(4.05);
                    assertThat(position.longitude()).isEqualTo(9.76);
                })
                .verifyComplete();
    }

    @Test
    void sends_the_tenant_id_as_a_query_param() throws InterruptedException {
        serviceBur.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("[]"));

        adapter.listerPositionsParTenant("tenant-bnft-ndjamena", "delegation-token-1").collectList().block();

        var requete = serviceBur.takeRequest();
        assertThat(requete.getPath()).isEqualTo("/api/v1/bur/positions?tenantId=tenant-bnft-ndjamena");
        assertThat(requete.getHeader("Authorization")).isEqualTo("Bearer delegation-token-1");
    }

    @Test
    void refuses_to_call_service_bur_without_a_delegation_token() {
        StepVerifier.create(adapter.listerPositionsParTenant("tenant-bgft-douala", null))
                .expectError(TrkServiceIndisponibleException.class)
                .verify();

        assertThat(serviceBur.getRequestCount()).isZero();
    }
}
