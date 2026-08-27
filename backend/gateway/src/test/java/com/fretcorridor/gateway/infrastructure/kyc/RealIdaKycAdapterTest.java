package com.fretcorridor.gateway.infrastructure.kyc;

import com.fretcorridor.gateway.domain.kyc.KycDossierIntrouvableException;
import com.fretcorridor.gateway.domain.kyc.KycServiceIndisponibleException;
import com.fretcorridor.gateway.domain.kyc.KycStatut;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class RealIdaKycAdapterTest {

    private MockWebServer serviceIda;
    private RealIdaKycAdapter adapter;

    @BeforeEach
    void demarrer() throws IOException {
        serviceIda = new MockWebServer();
        serviceIda.start();
        adapter = new RealIdaKycAdapter(WebClient.builder(), serviceIda.url("/").toString());
    }

    @AfterEach
    void arreter() throws IOException {
        serviceIda.shutdown();
    }

    @Test
    void lists_pending_with_delegation_token() throws InterruptedException {
        serviceIda.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        [{"acteurId":"a1","telephone":"+237600","nom":"Ngono","prenom":"Awa","raisonSociale":null,"niveauKyc":"NIVEAU_1","roles":["CHAUFFEUR"]}]
                        """));

        StepVerifier.create(adapter.listerEnAttente("tenant-bgft-douala", "tok").collectList())
                .assertNext(list -> {
                    assertThat(list).hasSize(1);
                    assertThat(list.get(0).id()).isEqualTo("a1");
                    assertThat(list.get(0).acteurNom()).isEqualTo("Ngono Awa");
                    assertThat(list.get(0).statut()).isEqualTo(KycStatut.EN_ATTENTE);
                })
                .verifyComplete();

        var req = serviceIda.takeRequest();
        assertThat(req.getPath()).isEqualTo("/api/ida/admin/kyc/pending?tenantId=tenant-bgft-douala");
        assertThat(req.getHeader("Authorization")).isEqualTo("Bearer tok");
    }

    @Test
    void detail_maps_presigned_urls() throws InterruptedException {
        serviceIda.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"acteurId":"a1","telephone":"+237600","nom":"Ngono","prenom":"Awa","raisonSociale":null,"niveauKyc":"NIVEAU_1","roles":["CHAUFFEUR"],"pieces":[{"id":"p1","typeDocument":"CNI","url":"https://minio/x","dateDepot":"2026-01-01T10:00:00"}]}
                        """));

        StepVerifier.create(adapter.detail("a1", "tenant-bgft-douala", "tok"))
                .assertNext(d -> {
                    assertThat(d.pieces()).hasSize(1);
                    assertThat(d.pieces().get(0).url()).isEqualTo("https://minio/x");
                })
                .verifyComplete();

        assertThat(serviceIda.takeRequest().getPath())
                .isEqualTo("/api/ida/admin/kyc/a1?tenantId=tenant-bgft-douala");
    }

    @Test
    void decision_validates_and_caches_idempotency() throws InterruptedException {
        serviceIda.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"acteurId":"a1","telephone":"+237600","nom":"Ngono","prenom":"Awa","raisonSociale":null,"niveauKyc":"NIVEAU_2","roles":["CHAUFFEUR"]}
                        """));

        var first = adapter.decider("a1", KycStatut.VALIDE, "idem-1", "tenant-bgft-douala", "tok", null).block();
        var replay = adapter.decider("a1", KycStatut.VALIDE, "idem-1", "tenant-bgft-douala", "tok", null).block();

        assertThat(first.niveauKyc()).isEqualTo("NIVEAU_2");
        assertThat(replay).isEqualTo(first);
        assertThat(serviceIda.getRequestCount()).isEqualTo(1);
    }

    @Test
    void unknown_acteur_becomes_404_domain_error() {
        serviceIda.enqueue(new MockResponse().setResponseCode(404));

        StepVerifier.create(adapter.detail("inconnu", "tenant-bgft-douala", "tok"))
                .expectError(KycDossierIntrouvableException.class)
                .verify();
    }

    @Test
    void maps_transporteur_role_when_chauffeur_also_present() throws InterruptedException {
        serviceIda.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        [{"acteurId":"a1","telephone":"+237696000001","nom":"Etoile","prenom":null,"raisonSociale":"Transport Étoile SARL","niveauKyc":"NIVEAU_1","roles":["TRANSPORTEUR","CHAUFFEUR"]}]
                        """));

        StepVerifier.create(adapter.listerEnAttente("tenant-bgft-douala", "tok").collectList())
                .assertNext(list -> assertThat(list.get(0).typeActeur()).isEqualTo("CHAUFFEUR"))
                .verifyComplete();
    }

    @Test
    void network_failure_maps_to_service_unavailable() {
        serviceIda.enqueue(new MockResponse().setResponseCode(503));

        StepVerifier.create(adapter.listerEnAttente("tenant-bgft-douala", "tok"))
                .expectError(KycServiceIndisponibleException.class)
                .verify();
    }
}
