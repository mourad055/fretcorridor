package com.flysoft.fretcorridor.cap.web;

import com.flysoft.fretcorridor.cap.domain.CategorieMarchandise;
import com.flysoft.fretcorridor.cap.domain.TarifChauffeur;
import com.flysoft.fretcorridor.cap.domain.TarifChauffeurService;
import com.flysoft.fretcorridor.cap.security.JwtService;
import com.flysoft.fretcorridor.cap.web.dto.TarifChauffeurCreationRequest;
import com.flysoft.fretcorridor.cap.web.dto.TarifChauffeurModificationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class TarifChauffeurControllerTest {

    private static final String TOKEN = "Bearer un-jwt-quelconque";
    private static final String CLE_INTERNE = "cle-de-test";

    @Mock private TarifChauffeurService tarifChauffeurService;
    @Mock private JwtService jwtService;

    private TarifChauffeurController controller;
    private UUID transporteurId;
    private UUID axeId;
    private String tenantId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new TarifChauffeurController(tarifChauffeurService, jwtService, CLE_INTERNE);
        transporteurId = UUID.randomUUID();
        axeId = UUID.randomUUID();
        tenantId = "tenant-bgft-douala";
        when(jwtService.extraireActeurId("un-jwt-quelconque")).thenReturn(transporteurId);
        when(jwtService.extraireTenantId("un-jwt-quelconque")).thenReturn(tenantId);
    }

    private TarifChauffeur unTarif() {
        return new TarifChauffeur(transporteurId, axeId, CategorieMarchandise.STANDARD, BigDecimal.valueOf(75), tenantId);
    }

    @Test
    void configurer_uses_the_transporteurId_from_the_jwt_not_the_request_body() {
        when(tarifChauffeurService.configurer(eq(transporteurId), eq(tenantId), eq(axeId),
                eq(CategorieMarchandise.STANDARD), eq(BigDecimal.valueOf(75))))
                .thenReturn(unTarif());

        var reponse = controller.configurer(
                new TarifChauffeurCreationRequest(axeId, CategorieMarchandise.STANDARD, BigDecimal.valueOf(75)), TOKEN);

        assertThat(reponse.axeId()).isEqualTo(axeId);
        assertThat(reponse.tarifFcfaParKg()).isEqualByComparingTo(BigDecimal.valueOf(75));
    }

    @Test
    void mesTarifs_lists_only_the_authenticated_driver_tariffs() {
        when(tarifChauffeurService.listerMesTarifs(transporteurId, tenantId)).thenReturn(List.of(unTarif()));

        var reponse = controller.mesTarifs(TOKEN);

        assertThat(reponse).hasSize(1);
    }

    @Test
    void modifier_rejects_when_the_tarif_does_not_belong_to_the_caller() {
        UUID id = UUID.randomUUID();
        when(tarifChauffeurService.modifierTarif(eq(id), eq(tenantId), any()))
                .thenThrow(new IllegalArgumentException("Tarif introuvable : " + id));

        assertThatThrownBy(() -> controller.modifier(id, new TarifChauffeurModificationRequest(BigDecimal.TEN), TOKEN))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void detail_requires_the_internal_service_key() {
        assertThatThrownBy(() -> controller.detail(transporteurId, axeId, CategorieMarchandise.STANDARD, "mauvaise-cle"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401");
    }

    @Test
    void detail_returns_404_when_no_active_tarif_exists() {
        when(tarifChauffeurService.detailPourAppelInterne(transporteurId, axeId, CategorieMarchandise.STANDARD))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.detail(transporteurId, axeId, CategorieMarchandise.STANDARD, CLE_INTERNE))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void intervalle_works_without_any_authentication() {
        when(tarifChauffeurService.intervallePourAppelInterne(axeId, CategorieMarchandise.STANDARD))
                .thenReturn(Optional.of(new TarifChauffeurService.IntervalleTarif(
                        BigDecimal.valueOf(70), BigDecimal.valueOf(115), 3)));

        var reponse = controller.intervalle(axeId, CategorieMarchandise.STANDARD);

        assertThat(reponse.tarifMinFcfaParKg()).isEqualByComparingTo(BigDecimal.valueOf(70));
        assertThat(reponse.tarifMaxFcfaParKg()).isEqualByComparingTo(BigDecimal.valueOf(115));
        assertThat(reponse.nombreChauffeurs()).isEqualTo(3);
    }
}
