package com.fretcorridor.mat.domain;

import com.fretcorridor.mat.web.dto.CoutLotResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoutCompositeServiceTest {

    @Mock
    private ModelePonderationRepository modelePonderationRepository;

    @Mock
    private PonderationCritereRepository ponderationCritereRepository;

    @Mock
    private CycleMatchingRepository cycleMatchingRepository;

    @InjectMocks
    private CoutCompositeService service;

    @Test
    void shouldFallbackToEqualWeightsWhenNoActiveModel() {
        UUID demandeId = UUID.randomUUID();
        UUID capaciteId = UUID.randomUUID();

        // Test sans axe specifique (axeId=null) : resoudreModele() saute directement
        // au repli sur le modele par defaut - meme cas "aucun modele actif" qu'avant,
        // exprime maintenant via findFirstByAxeIdIsNullAndActifTrue plutot que
        // l'ancienne findFirstByActifTrue (RG-106, ponderations par axe).
        when(modelePonderationRepository.findFirstByAxeIdIsNullAndActifTrue()).thenReturn(Optional.empty());
        when(cycleMatchingRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CandidatCout candidat = new CandidatCout(capaciteId, Map.of("A", 0.5, "B", 0.8));

        CoutLotResponse response = service.calculerCoutsLot(demandeId, null, List.of(candidat));

        assertThat(response).isNotNull();
        assertThat(response.modeDegrade()).isTrue();
        assertThat(response.versionModeleUtilisee()).isNull();
        assertThat(response.resultats()).hasSize(1);
        double expected = (0.5 + 0.8) / 2.0;
        double actual = response.resultats().get(0).coutTotal().doubleValue();
        assertThat(actual).isBetween(expected - 0.001, expected + 0.001);
    }

    @Test
    void shouldCalculateCostsWithActiveModel() {
        UUID demandeId = UUID.randomUUID();
        UUID capaciteId = UUID.randomUUID();
        UUID modeleId = UUID.randomUUID();

        ModelePonderation modele = org.mockito.Mockito.mock(ModelePonderation.class);
        when(modele.getId()).thenReturn(modeleId);
        when(modele.getVersion()).thenReturn(1);
        when(modelePonderationRepository.findFirstByAxeIdIsNullAndActifTrue()).thenReturn(Optional.of(modele));

        PonderationCritere critere1 = org.mockito.Mockito.mock(PonderationCritere.class);
        when(critere1.getCodeCritere()).thenReturn("A");
        when(critere1.getPoids()).thenReturn(BigDecimal.valueOf(0.5));

        PonderationCritere critere2 = org.mockito.Mockito.mock(PonderationCritere.class);
        when(critere2.getCodeCritere()).thenReturn("B");
        when(critere2.getPoids()).thenReturn(BigDecimal.valueOf(0.3));

        when(ponderationCritereRepository.findByModeleId(modeleId)).thenReturn(List.of(critere1, critere2));

        CandidatCout candidat = new CandidatCout(capaciteId, Map.of("A", 0.4, "B", 0.9));

        CycleMatching cycle = new CycleMatching(capaciteId, demandeId, modeleId,
                BigDecimal.valueOf(0.5 * 0.4 + 0.3 * 0.9),
                Map.of("A", 0.5 * 0.4, "B", 0.3 * 0.9),
                false);
        when(cycleMatchingRepository.saveAll(any())).thenReturn(List.of(cycle));

        CoutLotResponse response = service.calculerCoutsLot(demandeId, null, List.of(candidat));

        assertThat(response).isNotNull();
        assertThat(response.modeDegrade()).isFalse();
        assertThat(response.versionModeleUtilisee()).isEqualTo(1);
        assertThat(response.resultats()).hasSize(1);
        double expected = 0.5 * 0.4 + 0.3 * 0.9;
        double actual = response.resultats().get(0).coutTotal().doubleValue();
        assertThat(actual).isBetween(expected - 0.001, expected + 0.001);
    }
}
