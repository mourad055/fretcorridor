package com.fretcorridor.gateway.infrastructure.auth;

import com.fretcorridor.gateway.domain.Actor;
import com.fretcorridor.gateway.domain.AuthenticationServiceUnavailableException;
import com.fretcorridor.gateway.domain.InvalidCredentialsException;
import com.fretcorridor.gateway.domain.Role;
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
 * EF-IDA-01 (contrat réel, PIN et non OTP — écart documenté à part) : vérifie
 * ServiceIdaAuthenticationAdapter contre un vrai serveur HTTP local, sans
 * dépendre d'une instance service-ida réelle.
 */
class ServiceIdaAuthenticationAdapterTest {

    private MockWebServer serviceIda;
    private ServiceIdaAuthenticationAdapter adapter;

    @BeforeEach
    void demarrerServiceIdaFactice() throws IOException {
        serviceIda = new MockWebServer();
        serviceIda.start();
        adapter = new ServiceIdaAuthenticationAdapter(WebClient.builder(), serviceIda.url("/").toString());
    }

    @AfterEach
    void arreterServiceIdaFactice() throws IOException {
        serviceIda.shutdown();
    }

    @Test
    void maps_a_successful_login_to_an_actor() {
        serviceIda.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"accessToken":"t","refreshToken":"r","acteurId":"11111111-1111-1111-1111-111111111111","roles":["BUREAU"],"tenantId":"tenant-bgft-douala"}
                        """));

        StepVerifier.create(adapter.authenticate("+237600000001", "1234"))
                .expectNext(new Actor("11111111-1111-1111-1111-111111111111", "+237600000001", Role.BUREAU, "tenant-bgft-douala"))
                .verifyComplete();
    }

    @Test
    void maps_administration_role_to_the_gateway_admin_role() {
        serviceIda.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"accessToken":"t","refreshToken":"r","acteurId":"id-1","roles":["ADMINISTRATION"],"tenantId":"tenant-flysoft"}
                        """));

        StepVerifier.create(adapter.authenticate("+237600000003", "1234"))
                .expectNextMatches(actor -> actor.role() == Role.ADMIN)
                .verifyComplete();
    }

    @Test
    void picks_the_first_role_the_gateway_recognises_when_several_are_returned() {
        serviceIda.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"accessToken":"t","refreshToken":"r","acteurId":"id-1","roles":["CHAUFFEUR","TRANSPORTEUR"],"tenantId":"tenant-bgft-douala"}
                        """));

        StepVerifier.create(adapter.authenticate("+237600000002", "1234"))
                .expectNextMatches(actor -> actor.role() == Role.TRANSPORTEUR)
                .verifyComplete();
    }

    @Test
    void rejects_an_actor_with_no_role_the_gateway_manages() {
        serviceIda.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"accessToken":"t","refreshToken":"r","acteurId":"id-1","roles":["CHAUFFEUR"],"tenantId":"tenant-bgft-douala"}
                        """));

        StepVerifier.create(adapter.authenticate("+237600000009", "1234"))
                .expectError(InvalidCredentialsException.class)
                .verify();
    }

    @Test
    void maps_a_401_from_service_ida_to_invalid_credentials() {
        serviceIda.enqueue(new MockResponse().setResponseCode(401).setBody("PIN_INCORRECT:2"));

        StepVerifier.create(adapter.authenticate("+237600000001", "0000"))
                .expectError(InvalidCredentialsException.class)
                .verify();
    }

    @Test
    void maps_a_timeout_to_a_service_unavailable_error_not_invalid_credentials() {
        serviceIda.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBodyDelay(5, java.util.concurrent.TimeUnit.SECONDS)
                .setBody("{}"));

        StepVerifier.create(adapter.authenticate("+237600000001", "1234"))
                .expectError(AuthenticationServiceUnavailableException.class)
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void sends_telephone_and_pin_as_the_request_body() throws InterruptedException {
        serviceIda.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"accessToken":"t","refreshToken":"r","acteurId":"id-1","roles":["BUREAU"],"tenantId":"tenant-bgft-douala"}
                        """));

        adapter.authenticate("+237600000001", "4321").block();

        var requete = serviceIda.takeRequest();
        assertThat(requete.getPath()).isEqualTo("/api/auth/login");
        assertThat(requete.getBody().readUtf8()).contains("\"telephone\":\"+237600000001\"").contains("\"codePin\":\"4321\"");
    }
}
