package com.fretcorridor.opt.domain;

import com.fretcorridor.dto.PointGeoDto;
import com.fretcorridor.opt.client.CandidatCoutDto;
import com.fretcorridor.opt.client.CoutLotResponseDto;
import com.fretcorridor.opt.client.CoutResponseDto;
import com.fretcorridor.opt.client.ServiceCapClient;
import com.fretcorridor.opt.client.ServiceGeoClient;
import com.fretcorridor.opt.client.ServiceMatClient;
import com.fretcorridor.opt.client.ServiceNotClient;
import com.fretcorridor.opt.client.ValhallaClient;
import com.fretcorridor.opt.messaging.AffectationConfirmeeEvent;
import com.fretcorridor.opt.messaging.OptEventPublisher;
import com.fretcorridor.opt.messaging.PropositionEmiseEvent;
import com.fretcorridor.opt.tarification.TarificationL4Service;
import com.fretcorridor.opt.tarification.TarificationResultat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * RG-039/EF-MKT-07 (audit CDC du 19 août, "au plus trois propositions par
 * demande") : Kuhn-Munkres publie jusqu'à 2 alternatives (rang 2/3),
 * toujours de simples PropositionEmiseEvent informationnelles.
 *
 * UC-MAT-02 du CDC (page 43, "Notification, acceptation ou refus d'une
 * mission par le chauffeur", bug corrigé le 26/08) : le rang 1 n'est plus
 * auto-confirmé au sortir du solveur -- il devient une PropositionMission
 * EN_ATTENTE, notifiée au transporteur. Affectation/AffectationConfirmee/
 * réservation de capacité n'ont lieu qu'à l'acceptation explicite
 * (confirmerDepuisProposition, appelé par PropositionMissionService).
 */
class AffectationL1ServiceTest {

    @Mock private ServiceMatClient serviceMatClient;
    @Mock private ValhallaClient valhallaClient;
    @Mock private TarificationL4Service tarificationL4Service;
    @Mock private AffectationRepository affectationRepository;
    @Mock private PropositionMissionRepository propositionMissionRepository;
    @Mock private OptEventPublisher eventPublisher;
    @Mock private ServiceGeoClient serviceGeoClient;
    @Mock private ServiceCapClient serviceCapClient;
    @Mock private ServiceNotClient serviceNotClient;

    private AffectationL1Service service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AffectationL1Service(serviceMatClient, valhallaClient, tarificationL4Service,
                affectationRepository, propositionMissionRepository, eventPublisher,
                serviceGeoClient, serviceCapClient, serviceNotClient);
    }

    @Test
    void winning_candidate_becomes_a_pending_proposition_not_an_immediate_affectation() {
        UUID demandeId = UUID.randomUUID();
        UUID axeId = UUID.randomUUID();
        UUID capaciteMoinsChere = UUID.randomUUID();
        UUID capaciteIntermediaire = UUID.randomUUID();
        UUID capacitePlusChere = UUID.randomUUID();
        UUID transporteurId = UUID.randomUUID();

        // Candidats sans position connue : Valhalla est sauté (mode degrade),
        // ce qui simplifie ce test sans affecter le classement par cout.
        // transporteurId renseigné sur chaque candidat -- obligatoire pour
        // qu'une PropositionMission soit créée (UC-MAT-02 : personne à
        // notifier sinon, cf AffectationL1Service).
        List<CandidatCoutDto> candidats = List.of(
                new CandidatCoutDto(capacitePlusChere, transporteurId, null, null, null, null, "FOURGON"),
                new CandidatCoutDto(capaciteMoinsChere, transporteurId, null, null, null, null, "FOURGON"),
                new CandidatCoutDto(capaciteIntermediaire, transporteurId, null, null, null, null, "FOURGON"));

        DemandeAvecCandidats demande = new DemandeAvecCandidats(demandeId,
                new PointGeoDto(4.05, 9.7), new PointGeoDto(3.87, 11.52), axeId, BigDecimal.valueOf(1000), candidats, null, null, null, null, null, null, null, null);

        when(serviceMatClient.calculerCoutsLot(any())).thenReturn(new CoutLotResponseDto(demandeId, 1, false, List.of(
                new CoutResponseDto(capacitePlusChere, UUID.randomUUID(), BigDecimal.valueOf(30000)),
                new CoutResponseDto(capaciteMoinsChere, UUID.randomUUID(), BigDecimal.valueOf(10000)),
                new CoutResponseDto(capaciteIntermediaire, UUID.randomUUID(), BigDecimal.valueOf(20000))
        )));

        when(tarificationL4Service.calculer(eq(axeId), eq("FOURGON"), any(), any(), any()))
                .thenReturn(new TarificationResultat(null, null, "FORFAITAIRE", BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(10000), false,
                        BigDecimal.valueOf(10000), BigDecimal.valueOf(1000), BigDecimal.valueOf(9000), false));

        service.calculerAffectationOptimale(List.of(demande));

        // Rang 1 (capaciteMoinsChere, coût le plus bas) : PropositionMission
        // créée EN_ATTENTE, transporteur notifié -- PAS d'Affectation, PAS de
        // PropositionEmiseEvent pour ce rang à ce stade.
        ArgumentCaptor<PropositionMission> propositionCaptor = ArgumentCaptor.forClass(PropositionMission.class);
        verify(propositionMissionRepository).save(propositionCaptor.capture());
        PropositionMission proposition = propositionCaptor.getValue();
        assertThat(proposition.getCapaciteId()).isEqualTo(capaciteMoinsChere);
        assertThat(proposition.getTransporteurId()).isEqualTo(transporteurId);
        assertThat(proposition.getStatut()).isEqualTo(PropositionMission.Statut.EN_ATTENTE);
        assertThat(proposition.getPrixTransport()).isEqualByComparingTo("10000");
        assertThat(proposition.getExpireA()).isAfter(Instant.now());

        verify(serviceNotClient).notifier(eq(transporteurId), any(), any(), eq("PROPOSITION_MISSION"), any(), any());
        verifyNoInteractions(affectationRepository);
        verifyNoInteractions(serviceCapClient);

        // Rang 2/3 (alternatives) : comportement inchangé, publiées
        // immédiatement, informationnelles.
        ArgumentCaptor<PropositionEmiseEvent> captor = ArgumentCaptor.forClass(PropositionEmiseEvent.class);
        verify(eventPublisher, times(2)).publierPropositionEmise(captor.capture());

        List<PropositionEmiseEvent> alternatives = captor.getAllValues();
        PropositionEmiseEvent rang2 = alternatives.stream().filter(p -> p.rang() == 2).findFirst().orElseThrow();
        assertThat(rang2.capaciteId()).isEqualTo(capaciteIntermediaire);
        assertThat(rang2.missionId()).isNull();

        PropositionEmiseEvent rang3 = alternatives.stream().filter(p -> p.rang() == 3).findFirst().orElseThrow();
        assertThat(rang3.capaciteId()).isEqualTo(capacitePlusChere);
        assertThat(rang3.missionId()).isNull();
    }

    @Test
    void fewer_than_two_alternatives_yield_no_alternative_proposition() {
        UUID demandeId = UUID.randomUUID();
        UUID axeId = UUID.randomUUID();
        UUID seuleCapacite = UUID.randomUUID();
        UUID transporteurId = UUID.randomUUID();

        List<CandidatCoutDto> candidats = List.of(
                new CandidatCoutDto(seuleCapacite, transporteurId, null, null, null, null, "FOURGON"));

        DemandeAvecCandidats demande = new DemandeAvecCandidats(demandeId,
                new PointGeoDto(4.05, 9.7), new PointGeoDto(3.87, 11.52), axeId, BigDecimal.valueOf(1000), candidats, null, null, null, null, null, null, null, null);

        when(serviceMatClient.calculerCoutsLot(any())).thenReturn(new CoutLotResponseDto(demandeId, 1, false,
                List.of(new CoutResponseDto(seuleCapacite, UUID.randomUUID(), BigDecimal.valueOf(10000)))));

        when(tarificationL4Service.calculer(eq(axeId), eq("FOURGON"), any(), any(), any()))
                .thenReturn(new TarificationResultat(null, null, "FORFAITAIRE", BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(10000), false,
                        BigDecimal.valueOf(10000), BigDecimal.valueOf(1000), BigDecimal.valueOf(9000), false));

        service.calculerAffectationOptimale(List.of(demande));

        // Un seul candidat disponible -> une seule PropositionMission
        // (rang 1), aucune alternative forcée artificiellement (RG-039).
        verify(propositionMissionRepository).save(any(PropositionMission.class));
        verify(eventPublisher, never()).publierPropositionEmise(any());
    }

    @Test
    void winning_candidate_without_transporteur_id_creates_no_proposition() {
        // Degradation gracieuse (ENF-DIS-04) : sans transporteur resolu,
        // personne a notifier -- ne doit jamais planter le cycle.
        UUID demandeId = UUID.randomUUID();
        UUID axeId = UUID.randomUUID();
        UUID seuleCapacite = UUID.randomUUID();

        List<CandidatCoutDto> candidats = List.of(
                new CandidatCoutDto(seuleCapacite, null, null, null, null, null, "FOURGON"));

        DemandeAvecCandidats demande = new DemandeAvecCandidats(demandeId,
                new PointGeoDto(4.05, 9.7), new PointGeoDto(3.87, 11.52), axeId, BigDecimal.valueOf(1000), candidats, null, null, null, null, null, null, null, null);

        when(serviceMatClient.calculerCoutsLot(any())).thenReturn(new CoutLotResponseDto(demandeId, 1, false,
                List.of(new CoutResponseDto(seuleCapacite, UUID.randomUUID(), BigDecimal.valueOf(10000)))));

        when(tarificationL4Service.calculer(eq(axeId), eq("FOURGON"), any(), any(), any()))
                .thenReturn(new TarificationResultat(null, null, "FORFAITAIRE", BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(10000), false,
                        BigDecimal.valueOf(10000), BigDecimal.valueOf(1000), BigDecimal.valueOf(9000), false));

        service.calculerAffectationOptimale(List.of(demande));

        verifyNoInteractions(propositionMissionRepository);
        verifyNoInteractions(serviceNotClient);
    }

    @Test
    void confirmerDepuisProposition_creates_affectation_and_reserves_capacity() {
        UUID demandeId = UUID.randomUUID();
        UUID capaciteId = UUID.randomUUID();
        UUID transporteurId = UUID.randomUUID();
        UUID axeId = UUID.randomUUID();

        PropositionMission proposition = new PropositionMission(
                demandeId, capaciteId, transporteurId, null, "FOURGON", UUID.randomUUID(), axeId, 1,
                BigDecimal.valueOf(1000), "Douala", "Yaoundé",
                4.05, 9.7, 3.87, 11.52,
                150000.0, 7200.0, 600.0, "encodedPolyline",
                BigDecimal.valueOf(10000), BigDecimal.valueOf(10000),
                "Vrac", 1, "Jean", "690000000", "DOMICILE", "DES_QUE_POSSIBLE",
                1000.0, false, Instant.now().plusSeconds(900));

        when(tarificationL4Service.calculer(eq(axeId), eq("FOURGON"), any(), any(), any()))
                .thenReturn(new TarificationResultat(null, null, "FORFAITAIRE", BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(10000), false,
                        BigDecimal.valueOf(10000), BigDecimal.valueOf(1000), BigDecimal.valueOf(9000), false));

        when(affectationRepository.save(any(Affectation.class))).thenAnswer(inv -> {
            Affectation a = inv.getArgument(0);
            assignerId(a, UUID.randomUUID());
            return a;
        });

        service.confirmerDepuisProposition(proposition);

        verify(affectationRepository).save(any(Affectation.class));
        verify(serviceCapClient).reserver(eq(capaciteId), eq(BigDecimal.valueOf(1000)), any());

        ArgumentCaptor<AffectationConfirmeeEvent> captor = ArgumentCaptor.forClass(AffectationConfirmeeEvent.class);
        verify(eventPublisher).publierAffectationConfirmee(captor.capture());
        assertThat(captor.getValue().demandeId()).isEqualTo(demandeId);
        assertThat(captor.getValue().transporteurId()).isEqualTo(transporteurId);

        verify(eventPublisher).publierPropositionEmise(argThat(p -> p.rang() == 1 && p.missionId() != null));
    }

    // Affectation est volontairement sans setter (immuable, cf sa javadoc) --
    // id normalement assigne par Hibernate a l'INSERT reel, absent ici (pur
    // mock, pas de contexte de persistance). Reflexion = seul moyen propre
    // de simuler ce comportement dans ce test unitaire.
    private static void assignerId(Affectation affectation, UUID id) throws Exception {
        var champ = Affectation.class.getDeclaredField("id");
        champ.setAccessible(true);
        champ.set(affectation, id);
    }
}
