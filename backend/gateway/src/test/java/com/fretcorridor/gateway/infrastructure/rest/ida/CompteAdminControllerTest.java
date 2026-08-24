package com.fretcorridor.gateway.infrastructure.rest.ida;

import com.fretcorridor.gateway.domain.ida.CompteAdmin;
import com.fretcorridor.gateway.domain.ida.IdaCompteAdminPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Gestion des comptes par un Admin (audit UX 2026-08-23, §1.1). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
class CompteAdminControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private IdaCompteAdminPort idaCompteAdminPort;

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
    void a_bureau_actor_cannot_reach_account_management() {
        String token = tokenFor("+237600000001");

        webTestClient.get().uri("/api/v1/admin/comptes?tenantId=tenant-bgft-douala")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void an_admin_lists_accounts_of_the_requested_tenant() {
        String token = tokenFor("+237600000003");
        when(idaCompteAdminPort.listerParTenant(eq("tenant-bgft-douala"), any())).thenReturn(
                Flux.just(new CompteAdmin("c1", "+237690000001", "Nom", "Prenom", null, "tenant-bgft-douala",
                        Set.of("BUREAU"), true, "NIVEAU_1")));

        webTestClient.get().uri("/api/v1/admin/comptes?tenantId=tenant-bgft-douala")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Object.class)
                .hasSize(1);
    }

    @Test
    void an_admin_deactivates_an_account() {
        String token = tokenFor("+237600000003");
        when(idaCompteAdminPort.changerStatut(eq("c1"), eq("tenant-bgft-douala"), eq(false), any()))
                .thenReturn(Mono.just(new CompteAdmin("c1", "+237690000001", "Nom", "Prenom", null,
                        "tenant-bgft-douala", Set.of("BUREAU"), false, "NIVEAU_1")));

        webTestClient.put().uri("/api/v1/admin/comptes/c1/statut?tenantId=tenant-bgft-douala")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"actif\": false}")
                .exchange()
                .expectStatus().isOk();

        verify(idaCompteAdminPort).changerStatut(eq("c1"), eq("tenant-bgft-douala"), eq(false), any());
    }

    @Test
    void an_admin_changes_the_roles_of_an_account() {
        String token = tokenFor("+237600000003");
        when(idaCompteAdminPort.changerRoles(eq("c1"), eq("tenant-bgft-douala"), eq(Set.of("ADMINISTRATION")), any()))
                .thenReturn(Mono.just(new CompteAdmin("c1", "+237690000001", "Nom", "Prenom", null,
                        "tenant-bgft-douala", Set.of("ADMINISTRATION"), true, "NIVEAU_1")));

        webTestClient.put().uri("/api/v1/admin/comptes/c1/roles?tenantId=tenant-bgft-douala")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"roles\": [\"ADMINISTRATION\"]}")
                .exchange()
                .expectStatus().isOk();

        verify(idaCompteAdminPort).changerRoles(eq("c1"), eq("tenant-bgft-douala"), eq(Set.of("ADMINISTRATION")), any());
    }
}
