package com.fretcorridor.gateway.infrastructure.cap;

import com.fretcorridor.gateway.domain.cap.CapServiceIndisponibleException;
import com.fretcorridor.gateway.domain.cap.CapaciteRefuseeException;
import com.fretcorridor.gateway.domain.cap.DeclarationCapacite;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** EF-CAP-03 : vérifie RealCapaciteDeclarationAdapter contre un vrai serveur HTTP local. */
class RealCapaciteDeclarationAdapterTest {

    private MockWebServer serviceCap;
    private RealCapaciteDeclarationAdapter adapter;

    @BeforeEach
    void demarrerServiceCapFactice() throws IOException {
        serviceCap = new MockWebServer();
        serviceCap.start();
        adapter = new RealCapaciteDeclarationAdapter(WebClient.builder(), serviceCap.url("/").toString());
    }

    @AfterEach
    void arreterServiceCapFactice() throws IOException {
        serviceCap.shutdown();
    }

    private DeclarationCapacite requete() {
        return new DeclarationCapacite(
                "11111111-1111-1111-1111-111111111111", "22222222-2222-2222-2222-222222222222",
                "TOTALE", BigDecimal.valueOf(9500), null, null,
                4.05, 9.7, "Camion 10T",
                null, null, null, null, null, null, false,
                Instant.now().plus(1, ChronoUnit.DAYS));
    }

    @Test
    void declares_a_capacity_with_the_delegation_token() throws InterruptedException {
        serviceCap.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"id":"cap-1","vehiculeId":"11111111-1111-1111-1111-111111111111",
                         "axeId":"22222222-2222-2222-2222-222222222222","modeDeclaration":"TOTALE",
                         "poidsKg":9500,"poidsTaxableKg":9500,"capaciteResiduelleKg":9500,
                         "expiree":false,"publiee":true,
                         "dateDepart":"2026-08-13T00:00:00Z","dateCreation":"2026-08-12T00:00:00Z"}
                        """));

        StepVerifier.create(adapter.declarer(requete(), "delegation-token-1"))
                .expectNextMatches(c -> c.id().equals("cap-1") && c.publiee())
                .verifyComplete();

        var requeteRecue = serviceCap.takeRequest();
        assertThat(requeteRecue.getPath()).isEqualTo("/api/cap/capacites");
        assertThat(requeteRecue.getHeader("Authorization")).isEqualTo("Bearer delegation-token-1");
        assertThat(requeteRecue.getBody().readUtf8()).contains("\"typeVehicule\":\"Camion 10T\"");
    }

    @Test
    void refuses_to_call_service_cap_without_a_delegation_token() {
        StepVerifier.create(adapter.declarer(requete(), null))
                .expectError(CapServiceIndisponibleException.class)
                .verify();

        assertThat(serviceCap.getRequestCount()).isZero();
    }

    @Test
    void maps_a_400_to_a_refused_error() {
        serviceCap.enqueue(new MockResponse().setResponseCode(400).setBody("Validation échouée"));

        StepVerifier.create(adapter.declarer(requete(), "delegation-token-1"))
                .expectError(CapaciteRefuseeException.class)
                .verify();
    }

    @Test
    void maps_a_timeout_to_a_service_unavailable_error() {
        serviceCap.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBodyDelay(6, java.util.concurrent.TimeUnit.SECONDS)
                .setBody("{}"));

        StepVerifier.create(adapter.declarer(requete(), "delegation-token-1"))
                .expectError(CapServiceIndisponibleException.class)
                .verify(java.time.Duration.ofSeconds(10));
    }
}
