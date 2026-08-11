package com.fretcorridor.gateway.infrastructure.ida;

import com.fretcorridor.gateway.domain.ida.Profil;
import com.fretcorridor.gateway.domain.ida.ProfilCompletionRefuseeException;
import com.fretcorridor.gateway.domain.ida.ProfilServiceIndisponibleException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RG-011 : vérifie RealIdaProfilAdapter contre un vrai serveur HTTP local,
 * sans dépendre d'une instance service-ida réelle — même approche que
 * ServiceIdaAuthenticationAdapterTest.
 */
class RealIdaProfilAdapterTest {

    private MockWebServer serviceIda;
    private RealIdaProfilAdapter adapter;

    @BeforeEach
    void demarrerServiceIdaFactice() throws IOException {
        serviceIda = new MockWebServer();
        serviceIda.start();
        adapter = new RealIdaProfilAdapter(WebClient.builder(), serviceIda.url("/").toString());
    }

    @AfterEach
    void arreterServiceIdaFactice() throws IOException {
        serviceIda.shutdown();
    }

    @Test
    void reads_the_profile_with_the_delegation_token() throws InterruptedException {
        serviceIda.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"acteurId":"id-1","type":"PARTICULIER","nom":null,"prenom":null,"raisonSociale":null,"niveauKyc":"NIVEAU_0"}
                        """));

        StepVerifier.create(adapter.profil("delegation-token-1"))
                .expectNext(new Profil("id-1", "PARTICULIER", null, null, null, "NIVEAU_0"))
                .verifyComplete();

        var requete = serviceIda.takeRequest();
        assertThat(requete.getPath()).isEqualTo("/api/kyc/profil");
        assertThat(requete.getHeader("Authorization")).isEqualTo("Bearer delegation-token-1");
    }

    @Test
    void completes_the_individual_profile_and_extracts_it_from_the_completion_response() throws InterruptedException {
        serviceIda.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"accessToken":"t","refreshToken":"r","profil":{"acteurId":"id-1","type":"PARTICULIER","nom":"Ngono","prenom":"Awa","raisonSociale":null,"niveauKyc":"NIVEAU_1"}}
                        """));

        StepVerifier.create(adapter.completerParticulier("delegation-token-1", "Ngono", "Awa"))
                .expectNext(new Profil("id-1", "PARTICULIER", "Ngono", "Awa", null, "NIVEAU_1"))
                .verifyComplete();

        var requete = serviceIda.takeRequest();
        assertThat(requete.getPath()).isEqualTo("/api/kyc/profil/particulier");
        assertThat(requete.getMethod()).isEqualTo("PUT");
        assertThat(requete.getHeader("Authorization")).isEqualTo("Bearer delegation-token-1");
        assertThat(requete.getBody().readUtf8()).contains("\"nom\":\"Ngono\"").contains("\"prenom\":\"Awa\"");
    }

    @Test
    void completes_the_business_profile_without_sending_a_null_registration_number() throws InterruptedException {
        serviceIda.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"accessToken":"t","refreshToken":"r","profil":{"acteurId":"id-1","type":"ENTREPRISE","nom":null,"prenom":null,"raisonSociale":"Transco SA","niveauKyc":"NIVEAU_1"}}
                        """));

        StepVerifier.create(adapter.completerEntreprise("delegation-token-1", "Transco SA", null))
                .expectNext(new Profil("id-1", "ENTREPRISE", null, null, "Transco SA", "NIVEAU_1"))
                .verifyComplete();

        var requete = serviceIda.takeRequest();
        assertThat(requete.getBody().readUtf8())
                .contains("\"raisonSociale\":\"Transco SA\"")
                .doesNotContain("numeroRegistreCommerce");
    }

    @Test
    void maps_a_400_from_service_ida_to_a_completion_refused_error() {
        serviceIda.enqueue(new MockResponse().setResponseCode(400).setBody("ACTEUR_INTROUVABLE"));

        StepVerifier.create(adapter.completerParticulier("delegation-token-1", "Ngono", "Awa"))
                .expectErrorMatches(e -> e instanceof ProfilCompletionRefuseeException
                        && e.getMessage().equals("ACTEUR_INTROUVABLE"))
                .verify();
    }

    @Test
    void maps_a_timeout_to_a_service_unavailable_error() {
        serviceIda.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBodyDelay(5, java.util.concurrent.TimeUnit.SECONDS)
                .setBody("{}"));

        StepVerifier.create(adapter.profil("delegation-token-1"))
                .expectError(ProfilServiceIndisponibleException.class)
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void refuses_to_call_service_ida_without_a_delegation_token() {
        StepVerifier.create(adapter.profil(null))
                .expectError(ProfilServiceIndisponibleException.class)
                .verify();

        assertThat(serviceIda.getRequestCount()).isZero();
    }
}
