package com.fretcorridor.gateway.infrastructure.affiliation;

import com.fretcorridor.gateway.domain.affiliation.AffiliationRefuseeException;
import com.fretcorridor.gateway.domain.affiliation.AffiliationServiceIndisponibleException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;

/**
 * S18 (audit UX 2026-08-24) : jusqu'ici aucun test ne couvrait cet
 * adaptateur — inviter() mappait toute erreur, y compris un refus métier
 * 400 (numéro inconnu / acteur non transporteur), sur
 * AffiliationServiceIndisponibleException, masquant le vrai motif au Bureau
 * appelant. Corrigé pour distinguer 400 (refus) du reste (indisponible),
 * même principe que RealCapaciteDeclarationAdapter.
 */
class RealAffiliationAdapterTest {

    private MockWebServer serviceIda;
    private RealAffiliationAdapter adapter;

    @BeforeEach
    void demarrerServiceIdaFactice() throws IOException {
        serviceIda = new MockWebServer();
        serviceIda.start();
        adapter = new RealAffiliationAdapter(WebClient.builder(), serviceIda.url("/").toString());
    }

    @AfterEach
    void arreterServiceIdaFactice() throws IOException {
        serviceIda.shutdown();
    }

    @Test
    void refuses_to_call_service_ida_without_a_delegation_token() {
        StepVerifier.create(adapter.inviter(null, "+237690000001"))
                .expectError(AffiliationServiceIndisponibleException.class)
                .verify();
    }

    @Test
    void invites_a_transporter_with_the_delegation_token() throws InterruptedException {
        serviceIda.enqueue(new MockResponse().setResponseCode(201));

        StepVerifier.create(adapter.inviter("delegation-token-1", "+237690000001"))
                .verifyComplete();

        var request = serviceIda.takeRequest();
        org.assertj.core.api.Assertions.assertThat(request.getPath()).isEqualTo("/api/ida/affiliations");
        org.assertj.core.api.Assertions.assertThat(request.getHeader("Authorization")).isEqualTo("Bearer delegation-token-1");
    }

    @Test
    void maps_a_400_acteur_introuvable_to_a_refused_error_not_service_unavailable() {
        serviceIda.enqueue(new MockResponse().setResponseCode(400).setBody("ACTEUR_INTROUVABLE"));

        StepVerifier.create(adapter.inviter("delegation-token-1", "+237699999999"))
                .expectErrorMatches(e -> e instanceof AffiliationRefuseeException && e.getMessage().equals("ACTEUR_INTROUVABLE"))
                .verify();
    }

    @Test
    void maps_a_500_to_a_service_unavailable_error() {
        serviceIda.enqueue(new MockResponse().setResponseCode(500));

        StepVerifier.create(adapter.inviter("delegation-token-1", "+237690000001"))
                .expectError(AffiliationServiceIndisponibleException.class)
                .verify();
    }
}
