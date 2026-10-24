package com.fretcorridor.gateway.infrastructure.cap;

import com.fretcorridor.gateway.domain.cap.CapServiceIndisponibleException;
import com.fretcorridor.gateway.domain.cap.ConfigurerTarifChauffeur;
import com.fretcorridor.gateway.domain.cap.TarifChauffeurIntrouvableException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Plan §1bis (PR #145) : vérifie RealTarifChauffeurAdapter contre un vrai serveur HTTP local. */
class RealTarifChauffeurAdapterTest {

    private MockWebServer serviceCap;
    private RealTarifChauffeurAdapter adapter;

    @BeforeEach
    void demarrerServiceCapFactice() throws IOException {
        serviceCap = new MockWebServer();
        serviceCap.start();
        adapter = new RealTarifChauffeurAdapter(WebClient.builder(), serviceCap.url("/").toString());
    }

    @AfterEach
    void arreterServiceCapFactice() throws IOException {
        serviceCap.shutdown();
    }

    private static final String TARIF_JSON = """
            {"id":"tarif-1","axeId":"22222222-2222-2222-2222-222222222222",
             "categorieMarchandise":"STANDARD","tarifFcfaParKg":70.00,"actif":true,
             "dateCreation":"2026-10-24T00:00:00Z","dateMiseAJour":"2026-10-24T00:00:00Z"}
            """;

    @Test
    void configures_a_tarif_with_the_delegation_token() throws InterruptedException {
        serviceCap.enqueue(new MockResponse().setResponseCode(201)
                .setHeader("Content-Type", "application/json").setBody(TARIF_JSON));

        var requete = new ConfigurerTarifChauffeur("22222222-2222-2222-2222-222222222222", "STANDARD", BigDecimal.valueOf(70));
        StepVerifier.create(adapter.configurer(requete, "delegation-token-1"))
                .expectNextMatches(t -> t.id().equals("tarif-1") && t.actif())
                .verifyComplete();

        var requeteRecue = serviceCap.takeRequest();
        assertThat(requeteRecue.getPath()).isEqualTo("/api/cap/tarifs");
        assertThat(requeteRecue.getHeader("Authorization")).isEqualTo("Bearer delegation-token-1");
    }

    @Test
    void refuses_to_call_service_cap_without_a_delegation_token() {
        var requete = new ConfigurerTarifChauffeur("axe-1", "STANDARD", BigDecimal.TEN);
        StepVerifier.create(adapter.configurer(requete, null))
                .expectError(CapServiceIndisponibleException.class)
                .verify();

        assertThat(serviceCap.getRequestCount()).isZero();
    }

    @Test
    void lists_my_tarifs() throws InterruptedException {
        serviceCap.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json").setBody("[" + TARIF_JSON + "]"));

        StepVerifier.create(adapter.mesTarifs("delegation-token-1"))
                .expectNextMatches(t -> t.id().equals("tarif-1"))
                .verifyComplete();

        assertThat(serviceCap.takeRequest().getPath()).isEqualTo("/api/cap/tarifs/mes");
    }

    @Test
    void maps_a_404_on_modification_to_not_found() {
        serviceCap.enqueue(new MockResponse().setResponseCode(404).setBody("Tarif introuvable : tarif-x"));

        StepVerifier.create(adapter.modifier("tarif-x", BigDecimal.valueOf(80), "delegation-token-1"))
                .expectError(TarifChauffeurIntrouvableException.class)
                .verify();
    }

    @Test
    void maps_a_timeout_to_a_service_unavailable_error() {
        serviceCap.enqueue(new MockResponse().setResponseCode(200)
                .setBodyDelay(6, java.util.concurrent.TimeUnit.SECONDS).setBody("{}"));

        StepVerifier.create(adapter.mesTarifs("delegation-token-1"))
                .expectError(CapServiceIndisponibleException.class)
                .verify(java.time.Duration.ofSeconds(10));
    }

    @Test
    void reads_the_interval_without_any_token() throws InterruptedException {
        serviceCap.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"tarifMinFcfaParKg\":70.00,\"tarifMaxFcfaParKg\":100.00,\"nombreChauffeurs\":3}"));

        StepVerifier.create(adapter.intervalle("22222222-2222-2222-2222-222222222222", "STANDARD"))
                .expectNextMatches(i -> i.nombreChauffeurs() == 3)
                .verifyComplete();

        var requeteRecue = serviceCap.takeRequest();
        assertThat(requeteRecue.getPath()).contains("/api/cap/tarifs/intervalle");
        assertThat(requeteRecue.getHeader("Authorization")).isNull();
    }

    @Test
    void maps_a_404_on_the_interval_to_not_found() {
        serviceCap.enqueue(new MockResponse().setResponseCode(404).setBody("aucun chauffeur actif"));

        StepVerifier.create(adapter.intervalle("axe-1", "STANDARD"))
                .expectError(TarifChauffeurIntrouvableException.class)
                .verify();
    }
}
