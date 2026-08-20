package com.fretcorridor.gateway.infrastructure.rest.pay;

import com.fretcorridor.gateway.domain.adm.AdmPort;
import com.fretcorridor.gateway.domain.pay.DeclarationEspecesVue;
import com.fretcorridor.gateway.domain.pay.EcritureVue;
import com.fretcorridor.gateway.domain.pay.PayReadPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le PayReadPort réel appelle service-pay par HTTP — remplacé ici par un
 * double pour tester la lecture seule et le RBAC sans dépendre d'un service
 * externe démarré (cohérent avec l'isolation des tests des autres sprints).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
class PaiementReadControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private PayReadPort payReadPort;

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
    void a_transporteur_sees_its_own_solde_computed_from_its_history() {
        String token = tokenFor("+237600000002");
        when(payReadPort.ecrituresDuTransporteur(eq("a0000000-0000-0000-0000-000000000002"), any())).thenReturn(Flux.just(
                new EcritureVue("e1", "mission-1", "COMPTE_TRANSPORTEUR", "REVERSEMENT", "DEBIT", new BigDecimal("90"), Instant.now(), "VALIDE", null, false),
                new EcritureVue("e2", "mission-2", "COMPTE_TRANSPORTEUR", "REVERSEMENT", "DEBIT", new BigDecimal("60"), Instant.now(), "VALIDE", null, false)
        ));

        webTestClient.get().uri("/api/v1/transporteur/paiement")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.solde").isEqualTo(150)
                .jsonPath("$.historique.length()").isEqualTo(2);
    }

    @Test
    void a_non_transporteur_actor_can_still_reach_the_mobile_solde_endpoint() {
        String token = tokenFor("+237600000001"); // BUREAU — prouve l'absence de restriction de rôle sur /api/v1/paiement
        when(payReadPort.ecrituresDuTransporteur(eq("actor-bureau-1"), any())).thenReturn(Flux.empty());

        webTestClient.get().uri("/api/v1/paiement")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.solde").isEqualTo(0);
    }

    @Test
    void a_bureau_actor_cannot_reach_the_transporteur_paiement_endpoint() {
        String token = tokenFor("+237600000001");

        webTestClient.get().uri("/api/v1/transporteur/paiement")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void a_bureau_sees_the_financial_report_of_its_own_tenant() {
        String token = tokenFor("+237600000001");
        when(payReadPort.rapportDuTenant(eq("tenant-bgft-douala"), any())).thenReturn(Flux.just(
                new EcritureVue("e1", "mission-1", "COMPTE_SEQUESTRE_PRESTATAIRE", "ENCAISSEMENT", "CREDIT", new BigDecimal("500"), Instant.now(), "VALIDE", "VIREMENT", true)
        ));
        when(admPort.enregistrerAudit(any(), any(), any(), any(), any())).thenReturn(Mono.empty());

        webTestClient.get().uri("/api/v1/bureau/rapport-financier")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].modePaiement").isEqualTo("VIREMENT")
                .jsonPath("$[0].litigeActif").isEqualTo(true);

        verify(admPort).enregistrerAudit(eq("tenant-bgft-douala"), any(), eq("CONSULTATION_RAPPORT_FINANCIER"),
                eq("tenant:tenant-bgft-douala"), any());
    }

    @Test
    void an_admin_consulting_another_tenant_s_report_is_journalized() {
        String token = tokenFor("+237600000003");
        when(payReadPort.rapportDuTenant(any(), any())).thenReturn(Flux.empty());
        when(admPort.enregistrerAudit(any(), any(), any(), any(), any())).thenReturn(Mono.empty());

        webTestClient.get().uri("/api/v1/admin/rapport-financier/tenant-bnft-ndjamena")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk();

        verify(admPort).enregistrerAudit(eq("tenant-bnft-ndjamena"), any(), eq("CONSULTATION_RAPPORT_FINANCIER"),
                eq("tenant:tenant-bnft-ndjamena"), any());
    }

    @Test
    void a_transporteur_cannot_reach_the_admin_report_endpoint() {
        String token = tokenFor("+237600000002");

        webTestClient.get().uri("/api/v1/admin/rapport-financier/tenant-bgft-douala")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden();
    }

    /** EF-PAY-07 (S) : le Bureau voit les missions payées en espèces de son territoire, avec l'absence de protection signalée. */
    @Test
    void a_bureau_sees_cash_payments_with_no_protection_explicitly_signaled() {
        String token = tokenFor("+237600000001");
        when(payReadPort.paiementsEspecesDuTenant(eq("tenant-bgft-douala"), any())).thenReturn(Flux.just(
                new DeclarationEspecesVue("d1", "mission-especes-1", new BigDecimal("150"), Instant.now(), false)
        ));
        when(admPort.enregistrerAudit(any(), any(), any(), any(), any())).thenReturn(Mono.empty());

        webTestClient.get().uri("/api/v1/bureau/paiements-especes")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].missionId").isEqualTo("mission-especes-1")
                .jsonPath("$[0].protectionAssuree").isEqualTo(false);

        verify(admPort).enregistrerAudit(eq("tenant-bgft-douala"), any(), eq("CONSULTATION_PAIEMENTS_ESPECES"),
                eq("tenant:tenant-bgft-douala"), any());
    }

    @Test
    void an_admin_consulting_another_tenant_s_cash_payments_is_journalized() {
        String token = tokenFor("+237600000003");
        when(payReadPort.paiementsEspecesDuTenant(any(), any())).thenReturn(Flux.empty());
        when(admPort.enregistrerAudit(any(), any(), any(), any(), any())).thenReturn(Mono.empty());

        webTestClient.get().uri("/api/v1/admin/paiements-especes/tenant-bnft-ndjamena")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk();

        verify(admPort).enregistrerAudit(eq("tenant-bnft-ndjamena"), any(), eq("CONSULTATION_PAIEMENTS_ESPECES"),
                eq("tenant:tenant-bnft-ndjamena"), any());
    }
}
