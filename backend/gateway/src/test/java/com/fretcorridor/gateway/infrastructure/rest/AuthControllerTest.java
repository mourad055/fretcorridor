package com.fretcorridor.gateway.infrastructure.rest;

import com.fretcorridor.gateway.domain.Actor;
import com.fretcorridor.gateway.domain.AuthenticationPort;
import com.fretcorridor.gateway.domain.InvalidCredentialsException;
import com.fretcorridor.gateway.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
class AuthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private AuthenticationPort authenticationPort;

    @Test
    void login_with_valid_credentials_returns_a_token_and_the_role() {
        when(authenticationPort.authenticate("+237600000001", "123456"))
                .thenReturn(Mono.just(new Actor("actor-1", "+237600000001", Role.BUREAU, "tenant-bgft-douala")));

        webTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"phone": "+237600000001", "code": "123456"}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.token").isNotEmpty()
                .jsonPath("$.role").isEqualTo("BUREAU")
                .jsonPath("$.tenantId").isEqualTo("tenant-bgft-douala");
    }

    @Test
    void login_with_invalid_credentials_returns_401_problem_detail() {
        when(authenticationPort.authenticate(any(), any()))
                .thenReturn(Mono.error(new InvalidCredentialsException()));

        webTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"phone": "+237699999999", "code": "000000"}
                        """)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.title").isEqualTo("Authentification refusée");
    }

    @Test
    void login_without_phone_returns_400_problem_detail() {
        webTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"phone": "", "code": "123456"}
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }
}
