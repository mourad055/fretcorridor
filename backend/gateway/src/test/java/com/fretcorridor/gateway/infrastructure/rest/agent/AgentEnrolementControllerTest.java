package com.fretcorridor.gateway.infrastructure.rest.agent;

import com.fretcorridor.gateway.domain.agent.AgentEnrolementPort;
import com.fretcorridor.gateway.domain.agent.Enrolement;
import com.fretcorridor.gateway.domain.agent.EnrolementRefuseException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UC-IDA-03 : la route est réservée au rôle AGENT (RBAC gateway, défense en
 * profondeur en plus du contrôle côté service-ida). AgentEnrolementPort est
 * mocké : le comportement HTTP réel est couvert par RealAgentEnrolementAdapterTest.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
class AgentEnrolementControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private AgentEnrolementPort agentEnrolementPort;

    private String tokenFor(String phone) {
        return webTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"phone\": \"" + phone + "\", \"code\": \"123456\"}")
                .exchange()
                .expectStatus().isOk()
                .returnResult(Map.class)
                .getResponseBody()
                .blockFirst()
                .get("token")
                .toString();
    }

    @Test
    void an_agent_can_initiate_an_enrolment() {
        String token = tokenFor("+237600000006");
        when(agentEnrolementPort.initier(eq("mock-ida-delegation-token"), eq("+237600000010"), eq("CHAUFFEUR"),
                eq(4.05), eq(9.7), eq("idem-1")))
                .thenReturn(Mono.just(new Enrolement("enr-1", "+237600000010", "CHAUFFEUR", "EN_ATTENTE")));

        webTestClient.post().uri("/api/v1/agent/enrolements")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"telephone":"+237600000010","typeActeur":"CHAUFFEUR","latitude":4.05,"longitude":9.7,"idempotencyKey":"idem-1"}
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.statut").isEqualTo("EN_ATTENTE");
    }

    @Test
    void a_non_agent_actor_is_rejected_at_the_gateway() {
        String token = tokenFor("+237600000002"); // TRANSPORTEUR

        webTestClient.post().uri("/api/v1/agent/enrolements")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"telephone":"+237600000010","typeActeur":"CHAUFFEUR","latitude":4.05,"longitude":9.7,"idempotencyKey":"idem-1"}
                        """)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void an_agent_can_activate_an_enrolment() {
        String token = tokenFor("+237600000006");
        when(agentEnrolementPort.activer(eq("mock-ida-delegation-token"), eq("enr-1"), eq("654321"), eq("1234")))
                .thenReturn(Mono.just(new Enrolement("enr-1", "+237600000010", "CHAUFFEUR", "ACTIVE")));

        webTestClient.post().uri("/api/v1/agent/enrolements/enr-1/activation")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"otp\":\"654321\",\"codePin\":\"1234\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.statut").isEqualTo("ACTIVE");

        verify(agentEnrolementPort).activer("mock-ida-delegation-token", "enr-1", "654321", "1234");
    }

    @Test
    void a_refusal_from_service_ida_is_reported_as_bad_request() {
        String token = tokenFor("+237600000006");
        when(agentEnrolementPort.initier(any(), any(), any(), any(Double.class), any(Double.class), any()))
                .thenReturn(Mono.error(new EnrolementRefuseException("TELEPHONE_DEJA_UTILISE")));

        webTestClient.post().uri("/api/v1/agent/enrolements")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"telephone":"+237600000010","typeActeur":"CHAUFFEUR","latitude":4.05,"longitude":9.7,"idempotencyKey":"idem-1"}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.detail").isEqualTo("TELEPHONE_DEJA_UTILISE");
    }
}
