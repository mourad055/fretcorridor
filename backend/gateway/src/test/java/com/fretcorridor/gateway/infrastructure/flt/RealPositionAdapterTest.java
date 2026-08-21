package com.fretcorridor.gateway.infrastructure.flt;

import com.fretcorridor.gateway.domain.flt.FltServiceIndisponibleException;
import com.fretcorridor.gateway.domain.flt.PositionEnvoi;
import com.fretcorridor.gateway.domain.flt.PositionRefuseeException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class RealPositionAdapterTest {

    private MockWebServer serviceFlt;
    private RealPositionAdapter adapter;

    @BeforeEach
    void demarrerServiceFltFactice() throws IOException {
        serviceFlt = new MockWebServer();
        serviceFlt.start();
        adapter = new RealPositionAdapter(WebClient.builder(), serviceFlt.url("/").toString());
    }

    @AfterEach
    void arreterServiceFltFactice() throws IOException {
        serviceFlt.shutdown();
    }

    @Test
    void sends_a_position_with_the_delegation_token() throws InterruptedException {
        serviceFlt.enqueue(new MockResponse().setResponseCode(201));

        var position = new PositionEnvoi("mission-1", 4.05, 9.7, "2026-08-12T10:00:00", null, "GPS_NATIF", null);
        StepVerifier.create(adapter.envoyer("delegation-token-1", position)).verifyComplete();

        var requete = serviceFlt.takeRequest();
        assertThat(requete.getPath()).isEqualTo("/api/positions");
        assertThat(requete.getHeader("Authorization")).isEqualTo("Bearer delegation-token-1");
        assertThat(requete.getBody().readUtf8()).contains("\"missionId\":\"mission-1\"");
    }

    @Test
    void maps_a_400_to_a_refused_error() {
        serviceFlt.enqueue(new MockResponse().setResponseCode(400).setBody("MISSION_INTROUVABLE"));

        var position = new PositionEnvoi("mission-inconnue", 4.05, 9.7, "2026-08-12T10:00:00", null, "GPS_NATIF", null);
        StepVerifier.create(adapter.envoyer("delegation-token-1", position))
                .expectError(PositionRefuseeException.class)
                .verify();
    }

    @Test
    void refuses_to_call_service_flt_without_a_delegation_token() {
        var position = new PositionEnvoi("mission-1", 4.05, 9.7, "2026-08-12T10:00:00", null, "GPS_NATIF", null);
        StepVerifier.create(adapter.envoyer(null, position))
                .expectError(FltServiceIndisponibleException.class)
                .verify();

        assertThat(serviceFlt.getRequestCount()).isZero();
    }
}
