package com.fretcorridor.gateway.infrastructure.geo;

import com.fretcorridor.gateway.domain.geo.Axe;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.List;

/**
 * Caractérise le comportement RÉEL de RealGeoAdapter — pas un bug à
 * corriger ici, mais la limite acceptée en Phase 1 (cf. docs/adr, décision
 * mono-tenant GEO, et le Javadoc de RealGeoAdapter). Ce test échouerait le
 * jour où service-geo commence à filtrer réellement par tenant — c'est
 * voulu : ce sera le signal qu'AxeControllerIsolationTest doit revenir
 * vérifier RealGeoAdapter directement plutôt que la fixture MockGeoAdapter.
 */
class RealGeoAdapterTest {

    private MockWebServer serviceGeo;
    private RealGeoAdapter adapter;

    @BeforeEach
    void demarrerServiceGeoFactice() throws IOException {
        serviceGeo = new MockWebServer();
        serviceGeo.start();
        adapter = new RealGeoAdapter(WebClient.builder(), serviceGeo.url("/").toString());
    }

    @AfterEach
    void arreterServiceGeoFactice() throws IOException {
        serviceGeo.shutdown();
    }

    @Test
    void does_not_filter_by_tenant_server_side_it_only_labels_whatever_it_receives() {
        // service-geo réel (Phase 1) ne connaît qu'un tenant et ne filtre rien :
        // GET /api/geo/axes renvoie systématiquement tous les axes qu'il porte.
        serviceGeo.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        [{"id":"axe-1","hubOrigineNom":"Douala","hubDestinationNom":"Yaoundé","visibiliteActive":true,"matchingActif":true,"paiementActif":true}]
                        """));

        StepVerifier.create(adapter.listerAxesParTenant("tenant-nimporte-lequel").collectList())
                .assertNext(axes -> {
                    // Preuve du comportement documenté : le tenantId demandé est collé
                    // tel quel sur la réponse de service-geo, jamais vérifié contre elle.
                    List<Axe> attendu = List.of(
                            new Axe("axe-1", "tenant-nimporte-lequel", "Douala", "Yaoundé", 0.0, true, true, true));
                    org.assertj.core.api.Assertions.assertThat(axes).isEqualTo(attendu);
                })
                .verifyComplete();
    }
}
