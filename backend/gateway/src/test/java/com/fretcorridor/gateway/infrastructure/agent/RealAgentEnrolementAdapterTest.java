package com.fretcorridor.gateway.infrastructure.agent;

import com.fretcorridor.gateway.domain.agent.AgentServiceIndisponibleException;
import com.fretcorridor.gateway.domain.agent.Enrolement;
import com.fretcorridor.gateway.domain.agent.EnrolementIntrouvableException;
import com.fretcorridor.gateway.domain.agent.EnrolementRefuseException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/** UC-IDA-03 : vérifie RealAgentEnrolementAdapter contre un vrai serveur HTTP local. */
class RealAgentEnrolementAdapterTest {

    private MockWebServer serviceIda;
    private RealAgentEnrolementAdapter adapter;

    @BeforeEach
    void demarrerServiceIdaFactice() throws IOException {
        serviceIda = new MockWebServer();
        serviceIda.start();
        adapter = new RealAgentEnrolementAdapter(WebClient.builder(), serviceIda.url("/").toString());
    }

    @AfterEach
    void arreterServiceIdaFactice() throws IOException {
        serviceIda.shutdown();
    }

    @Test
    void initiates_an_enrolment_forwarding_the_agent_delegation_token() throws InterruptedException {
        serviceIda.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"enrolementId":"enr-1","telephone":"+237600000010","typeActeur":"CHAUFFEUR","statut":"EN_ATTENTE"}
                        """));

        StepVerifier.create(adapter.initier("delegation-token-1", "+237600000010", "CHAUFFEUR", 4.05, 9.7, "idem-1"))
                .expectNext(new Enrolement("enr-1", "+237600000010", "CHAUFFEUR", "EN_ATTENTE"))
                .verifyComplete();

        var requete = serviceIda.takeRequest();
        assertThat(requete.getPath()).isEqualTo("/api/agent/enrolements");
        assertThat(requete.getHeader("Authorization")).isEqualTo("Bearer delegation-token-1");
        assertThat(requete.getBody().readUtf8())
                .contains("\"telephone\":\"+237600000010\"")
                .contains("\"idempotencyKey\":\"idem-1\"");
    }

    @Test
    void activates_an_enrolment_with_the_otp_and_the_chosen_pin() throws InterruptedException {
        serviceIda.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"enrolementId":"enr-1","telephone":"+237600000010","typeActeur":"CHAUFFEUR","statut":"ACTIVE"}
                        """));

        StepVerifier.create(adapter.activer("delegation-token-1", "enr-1", "654321", "1234"))
                .expectNext(new Enrolement("enr-1", "+237600000010", "CHAUFFEUR", "ACTIVE"))
                .verifyComplete();

        var requete = serviceIda.takeRequest();
        assertThat(requete.getPath()).isEqualTo("/api/agent/enrolements/enr-1/activation");
        assertThat(requete.getBody().readUtf8()).contains("\"otp\":\"654321\"").contains("\"codePin\":\"1234\"");
    }

    @Test
    void maps_a_400_to_an_enrolement_refused_error() {
        serviceIda.enqueue(new MockResponse().setResponseCode(400).setBody("TELEPHONE_DEJA_UTILISE"));

        StepVerifier.create(adapter.initier("delegation-token-1", "+237600000010", "CHAUFFEUR", 4.05, 9.7, "idem-1"))
                .expectErrorMatches(e -> e instanceof EnrolementRefuseException
                        && e.getMessage().equals("TELEPHONE_DEJA_UTILISE"))
                .verify();
    }

    @Test
    void maps_a_404_on_activation_to_an_enrolement_not_found_error() {
        serviceIda.enqueue(new MockResponse().setResponseCode(404));

        StepVerifier.create(adapter.activer("delegation-token-1", "enr-inconnu", "654321", "1234"))
                .expectError(EnrolementIntrouvableException.class)
                .verify();
    }

    @Test
    void refuses_to_call_service_ida_without_a_delegation_token() {
        StepVerifier.create(adapter.initier(null, "+237600000010", "CHAUFFEUR", 4.05, 9.7, "idem-1"))
                .expectError(AgentServiceIndisponibleException.class)
                .verify();

        assertThat(serviceIda.getRequestCount()).isZero();
    }
}
