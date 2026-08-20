package com.fretcorridor.gateway.infrastructure.rest.adm;

import com.fretcorridor.gateway.domain.adm.AdmPort;
import com.fretcorridor.gateway.domain.adm.ConfigurationVue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** FE-ADM-03 (Sprint 10) : console de configuration, RBAC ADMIN, auteur pris de l'acteur authentifié. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
class ConfigurationControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private AdmPort admPort;

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
    void a_transporteur_cannot_reach_the_configuration_console() {
        String token = tokenFor("+237600000002");

        webTestClient.get().uri("/api/v1/admin/configurations/seuil-agregation-bur")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void definir_a_configuration_uses_the_authenticated_actor_as_author() {
        String token = tokenFor("+237600000003");
        when(admPort.definirConfiguration(eq("seuil-agregation-bur"), eq("GLOBAL"), eq("5"), eq("actor-admin-1"), any()))
                .thenReturn(Mono.just(new ConfigurationVue("seuil-agregation-bur", "GLOBAL", "5", "actor-admin-1", 1, Instant.now())));

        webTestClient.put().uri("/api/v1/admin/configurations/seuil-agregation-bur")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"valeur\": \"5\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.auteur").isEqualTo("actor-admin-1");

        verify(admPort).definirConfiguration(eq("seuil-agregation-bur"), eq("GLOBAL"), eq("5"), eq("actor-admin-1"), any());
    }

    /** EF-ADM-06 : le catalogue liste les clés déjà configurées, sans avoir à connaître leur nom à l'avance. */
    @Test
    void returns_the_catalogue_of_already_configured_keys() {
        String token = tokenFor("+237600000003");
        when(admPort.catalogueConfigurations(any())).thenReturn(Flux.just(
                new ConfigurationVue("grille-decision", "GLOBAL", "1", "actor-admin-1", 1, Instant.now())));

        webTestClient.get().uri("/api/v1/admin/configurations")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].cle").isEqualTo("grille-decision");
    }

    @Test
    void a_transporteur_cannot_reach_the_configuration_catalogue() {
        String token = tokenFor("+237600000002");

        webTestClient.get().uri("/api/v1/admin/configurations")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden();
    }
}
