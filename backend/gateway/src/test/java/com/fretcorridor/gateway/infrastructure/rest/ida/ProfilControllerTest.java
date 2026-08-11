package com.fretcorridor.gateway.infrastructure.rest.ida;

import com.fretcorridor.gateway.domain.ida.IdaProfilPort;
import com.fretcorridor.gateway.domain.ida.Profil;
import com.fretcorridor.gateway.domain.ida.ProfilCompletionRefuseeException;
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
 * RG-011 : la route est accessible à tout acteur authentifié (pas de restriction
 * de rôle), et transmet bien le delegationToken de l'acteur au port — jamais
 * un jeton du gateway. IdaProfilPort est mocké : le comportement HTTP réel est
 * couvert par RealIdaProfilAdapterTest.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
class ProfilControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private IdaProfilPort idaProfilPort;

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
    void an_authenticated_actor_reads_their_own_profile() {
        String token = tokenFor("+237600000002");
        when(idaProfilPort.profil("mock-ida-delegation-token"))
                .thenReturn(Mono.just(new Profil("id-1", "PARTICULIER", null, null, null, "NIVEAU_0")));

        webTestClient.get().uri("/api/v1/kyc/profil")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.niveauKyc").isEqualTo("NIVEAU_0");
    }

    @Test
    void an_unauthenticated_request_is_rejected() {
        webTestClient.get().uri("/api/v1/kyc/profil")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void completing_the_individual_profile_forwards_the_actor_delegation_token() {
        String token = tokenFor("+237600000002");
        when(idaProfilPort.completerParticulier(eq("mock-ida-delegation-token"), eq("Ngono"), eq("Awa")))
                .thenReturn(Mono.just(new Profil("id-1", "PARTICULIER", "Ngono", "Awa", null, "NIVEAU_1")));

        webTestClient.put().uri("/api/v1/kyc/profil/particulier")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"nom\": \"Ngono\", \"prenom\": \"Awa\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.niveauKyc").isEqualTo("NIVEAU_1");

        verify(idaProfilPort).completerParticulier("mock-ida-delegation-token", "Ngono", "Awa");
    }

    @Test
    void completing_the_individual_profile_without_a_last_name_is_rejected() {
        String token = tokenFor("+237600000002");

        webTestClient.put().uri("/api/v1/kyc/profil/particulier")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"nom\": \"\", \"prenom\": \"Awa\"}")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void completing_the_business_profile_without_a_registration_number_still_works() {
        String token = tokenFor("+237600000002");
        when(idaProfilPort.completerEntreprise(eq("mock-ida-delegation-token"), eq("Transco SA"), eq(null)))
                .thenReturn(Mono.just(new Profil("id-1", "ENTREPRISE", null, null, "Transco SA", "NIVEAU_1")));

        webTestClient.put().uri("/api/v1/kyc/profil/entreprise")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"raisonSociale\": \"Transco SA\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.raisonSociale").isEqualTo("Transco SA");
    }

    @Test
    void a_refusal_from_service_ida_is_reported_as_bad_request() {
        String token = tokenFor("+237600000002");
        when(idaProfilPort.completerParticulier(any(), any(), any()))
                .thenReturn(Mono.error(new ProfilCompletionRefuseeException("ACTEUR_INTROUVABLE")));

        webTestClient.put().uri("/api/v1/kyc/profil/particulier")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"nom\": \"Ngono\", \"prenom\": \"Awa\"}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.detail").isEqualTo("ACTEUR_INTROUVABLE");
    }
}
