package com.fretcorridor.gateway.infrastructure.rest.adm;

import com.fretcorridor.gateway.domain.adm.AdmPort;
import com.fretcorridor.gateway.domain.adm.TenantVue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** FE-ADM-04 (Sprint 10) : gestion des tenants, RBAC ADMIN. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
class TenantControllerTest {

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
    void a_bureau_actor_cannot_reach_tenant_management() {
        String token = tokenFor("+237600000001");

        webTestClient.get().uri("/api/v1/admin/tenants")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void an_admin_lists_tenants() {
        String token = tokenFor("+237600000003");
        when(admPort.tenants(any())).thenReturn(Flux.just(new TenantVue("tenant-bgft-douala", "Bureau Douala", "Cameroun", true)));

        webTestClient.get().uri("/api/v1/admin/tenants")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Object.class)
                .hasSize(1);
    }

    @Test
    void creer_un_tenant_uses_the_authenticated_actor_as_author() {
        String token = tokenFor("+237600000003");
        when(admPort.creerTenant(eq("tenant-new"), eq("Bureau Neuf"), eq("Tchad"), eq("actor-admin-1"), any()))
                .thenReturn(Mono.just(new TenantVue("tenant-new", "Bureau Neuf", "Tchad", true)));

        webTestClient.post().uri("/api/v1/admin/tenants")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"id\": \"tenant-new\", \"nom\": \"Bureau Neuf\", \"pays\": \"Tchad\"}")
                .exchange()
                .expectStatus().isOk();

        verify(admPort).creerTenant(eq("tenant-new"), eq("Bureau Neuf"), eq("Tchad"), eq("actor-admin-1"), any());
    }

    @Test
    void modifier_un_tenant_relays_nom_pays_et_statut() {
        String token = tokenFor("+237600000003");
        when(admPort.modifierTenant(eq("tenant-bgft-douala"), eq("Bureau Douala renommé"), eq("Cameroun"), eq(false), any()))
                .thenReturn(Mono.just(new TenantVue("tenant-bgft-douala", "Bureau Douala renommé", "Cameroun", false)));

        webTestClient.put().uri("/api/v1/admin/tenants/{id}", "tenant-bgft-douala")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"nom\": \"Bureau Douala renommé\", \"pays\": \"Cameroun\", \"actif\": false}")
                .exchange()
                .expectStatus().isOk();

        verify(admPort).modifierTenant(eq("tenant-bgft-douala"), eq("Bureau Douala renommé"), eq("Cameroun"), eq(false), any());
    }

    @Test
    void a_bureau_actor_cannot_modify_a_tenant() {
        String token = tokenFor("+237600000001");

        webTestClient.put().uri("/api/v1/admin/tenants/{id}", "tenant-bgft-douala")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"nom\": \"X\", \"pays\": \"Y\", \"actif\": true}")
                .exchange()
                .expectStatus().isForbidden();
    }
}
