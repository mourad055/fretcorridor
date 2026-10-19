package com.flysoft.fretcorridor.cap.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TarifChauffeurServiceTest {

    @Mock private TarifChauffeurRepository tarifChauffeurRepository;

    private TarifChauffeurService service;
    private UUID transporteurId;
    private UUID axeId;
    private String tenantId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new TarifChauffeurService(tarifChauffeurRepository);
        transporteurId = UUID.randomUUID();
        axeId = UUID.randomUUID();
        tenantId = "tenant-bgft-douala";
        when(tarifChauffeurRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void configurer_creates_a_new_tarif_when_none_exists_yet() {
        when(tarifChauffeurRepository.findByTransporteurIdAndAxeIdAndCategorieMarchandise(
                transporteurId, axeId, CategorieMarchandise.STANDARD)).thenReturn(Optional.empty());

        TarifChauffeur tarif = service.configurer(transporteurId, tenantId, axeId, CategorieMarchandise.STANDARD, BigDecimal.valueOf(75));

        assertThat(tarif.getTarifFcfaParKg()).isEqualByComparingTo(BigDecimal.valueOf(75));
        verify(tarifChauffeurRepository).save(any());
    }

    @Test
    void configurer_updates_the_existing_tarif_instead_of_duplicating_it() {
        TarifChauffeur existant = new TarifChauffeur(transporteurId, axeId, CategorieMarchandise.STANDARD, BigDecimal.valueOf(70), tenantId);
        when(tarifChauffeurRepository.findByTransporteurIdAndAxeIdAndCategorieMarchandise(
                transporteurId, axeId, CategorieMarchandise.STANDARD)).thenReturn(Optional.of(existant));

        TarifChauffeur resultat = service.configurer(transporteurId, tenantId, axeId, CategorieMarchandise.STANDARD, BigDecimal.valueOf(90));

        assertThat(resultat).isSameAs(existant);
        assertThat(resultat.getTarifFcfaParKg()).isEqualByComparingTo(BigDecimal.valueOf(90));
        verify(tarifChauffeurRepository, never()).save(any());
    }

    @Test
    void modifierTarif_rejects_a_tarif_belonging_to_another_tenant() {
        UUID id = UUID.randomUUID();
        TarifChauffeur tarif = new TarifChauffeur(transporteurId, axeId, CategorieMarchandise.STANDARD, BigDecimal.valueOf(70), "autre-tenant");
        when(tarifChauffeurRepository.findById(id)).thenReturn(Optional.of(tarif));

        assertThatThrownBy(() -> service.modifierTarif(id, tenantId, BigDecimal.valueOf(80)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("introuvable");
    }

    @Test
    void intervallePourAppelInterne_returns_the_min_and_max_among_active_drivers_only() {
        List<TarifChauffeur> actifs = List.of(
                new TarifChauffeur(UUID.randomUUID(), axeId, CategorieMarchandise.STANDARD, BigDecimal.valueOf(115), tenantId),
                new TarifChauffeur(UUID.randomUUID(), axeId, CategorieMarchandise.STANDARD, BigDecimal.valueOf(70), tenantId),
                new TarifChauffeur(UUID.randomUUID(), axeId, CategorieMarchandise.STANDARD, BigDecimal.valueOf(95), tenantId));
        when(tarifChauffeurRepository.findByAxeIdAndCategorieMarchandiseAndActifTrue(axeId, CategorieMarchandise.STANDARD))
                .thenReturn(actifs);

        var intervalle = service.intervallePourAppelInterne(axeId, CategorieMarchandise.STANDARD).orElseThrow();

        assertThat(intervalle.tarifMinFcfaParKg()).isEqualByComparingTo(BigDecimal.valueOf(70));
        assertThat(intervalle.tarifMaxFcfaParKg()).isEqualByComparingTo(BigDecimal.valueOf(115));
        assertThat(intervalle.nombreChauffeurs()).isEqualTo(3);
    }

    @Test
    void intervallePourAppelInterne_returns_empty_when_no_driver_is_active() {
        when(tarifChauffeurRepository.findByAxeIdAndCategorieMarchandiseAndActifTrue(axeId, CategorieMarchandise.STANDARD))
                .thenReturn(List.of());

        assertThat(service.intervallePourAppelInterne(axeId, CategorieMarchandise.STANDARD)).isEmpty();
    }
}
