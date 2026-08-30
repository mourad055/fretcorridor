package com.fretcorridor.opt.domain;

import com.fretcorridor.dto.PointGeoDto;
import com.fretcorridor.opt.client.CandidatCoutDto;
import com.fretcorridor.opt.client.CoutLotResponseDto;
import com.fretcorridor.opt.client.CoutResponseDto;
import com.fretcorridor.opt.client.ServiceCapClient;
import com.fretcorridor.opt.client.ServiceGeoClient;
import com.fretcorridor.opt.client.ServiceMatClient;
import com.fretcorridor.opt.client.ValhallaClient;
import com.fretcorridor.opt.messaging.OptEventPublisher;
import com.fretcorridor.opt.messaging.PropositionDiffuseeChauffeurEvent;
import com.fretcorridor.opt.messaging.PropositionEmiseEvent;
import com.fretcorridor.opt.tarification.TarificationL4Service;
import com.fretcorridor.opt.tarification.TarificationResultat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * RG-039/EF-MKT-07 (audit CDC du 19 août, "au plus trois propositions par
 * demande") : Kuhn-Munkres ne publiait qu'une seule PropositionEmise (rang
 * 1, codé en dur) -- ce test prouve que jusqu'à 2 alternatives supplémentaires
 * (rang 2/3) sont désormais publiées, classées par coût, sans toucher au
 * comportement rang 1 existant (Affectation/AffectationConfirmee inchangés).
 */
class AffectationL1ServiceTest {

    @Mock private ServiceMatClient serviceMatClient;
    @Mock private ValhallaClient valhallaClient;
    @Mock private TarificationL4Service tarificationL4Service;
    @Mock private AffectationRepository affectationRepository;
    @Mock private OptEventPublisher eventPublisher;
    @Mock private ServiceGeoClient serviceGeoClient;
    @Mock private ServiceCapClient serviceCapClient;
    @Mock private CompatibiliteMarchandisesService compatibiliteMarchandisesService;

    private AffectationL1Service service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AffectationL1Service(serviceMatClient, valhallaClient, tarificationL4Service,
                affectationRepository, eventPublisher, compatibiliteMarchandisesService, 900000L);
        // Le filtrage marchandises n'est pas l'objet de ce test : jamais
        // d'exclusion ici (retourne true par defaut en production en
        // l'absence de config + de donnees lot, cf CompatibiliteMarchandisesService).
        when(compatibiliteMarchandisesService.compatibleAvecDemandesDeLaCapacite(any(), any()))
                .thenReturn(true);
    }

    @Test
    void la_demande_est_diffusee_a_tous_les_chauffeurs_compatibles() {
        UUID demandeId = UUID.randomUUID();
        UUID axeId = UUID.randomUUID();
        UUID capaciteMoinsChere = UUID.randomUUID();
        UUID capaciteIntermediaire = UUID.randomUUID();
        UUID capacitePlusChere = UUID.randomUUID();

        // Candidats sans position connue : Valhalla est sauté (mode degrade),
        // ce qui simplifie ce test sans affecter le classement par cout.
        List<CandidatCoutDto> candidats = List.of(
                new CandidatCoutDto(capacitePlusChere, null, null, null, null, null, "FOURGON"),
                new CandidatCoutDto(capaciteMoinsChere, null, null, null, null, null, "FOURGON"),
                new CandidatCoutDto(capaciteIntermediaire, null, null, null, null, null, "FOURGON"));

        DemandeAvecCandidats demande = new DemandeAvecCandidats(demandeId,
                new PointGeoDto(4.05, 9.7), new PointGeoDto(3.87, 11.52), axeId, BigDecimal.valueOf(1000), candidats, null, null, null, null, null, null, null, null);

        when(serviceMatClient.calculerCoutsLot(any())).thenReturn(new CoutLotResponseDto(demandeId, 1, false, List.of(
                new CoutResponseDto(capacitePlusChere, UUID.randomUUID(), BigDecimal.valueOf(30000)),
                new CoutResponseDto(capaciteMoinsChere, UUID.randomUUID(), BigDecimal.valueOf(10000)),
                new CoutResponseDto(capaciteIntermediaire, UUID.randomUUID(), BigDecimal.valueOf(20000))
        )));

        when(affectationRepository.save(any(Affectation.class))).thenAnswer(inv -> {
            Affectation a = inv.getArgument(0);
            assignerId(a, UUID.randomUUID());
            return a;
        });

        when(tarificationL4Service.calculer(eq(axeId), eq("FOURGON"), any(), any(), any()))
                .thenReturn(new TarificationResultat(null, null, "FORFAITAIRE", BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(10000), false,
                        BigDecimal.valueOf(10000), BigDecimal.valueOf(1000), BigDecimal.valueOf(9000), false));

        service.calculerAffectationOptimale(List.of(demande));

        ArgumentCaptor<PropositionEmiseEvent> captor = ArgumentCaptor.forClass(PropositionEmiseEvent.class);
        verify(eventPublisher, times(3)).publierPropositionEmise(captor.capture());

        List<PropositionEmiseEvent> propositions = captor.getAllValues();
        assertThat(propositions).hasSize(3);

        // Diffusion-course (plan de reorientation) : la demande est diffusee
        // a TOUS les chauffeurs compatibles (premier arrive gagne), plus de
        // classement Kuhn-Munkres des 3 meilleures. Chaque proposition porte
        // donc un missionId (une Affectation est creee par candidat) et le
        // meme motif explicite.
        for (PropositionEmiseEvent proposition : propositions) {
            assertThat(proposition.missionId()).isNotNull();
            assertThat(proposition.motifClassement())
                    .isEqualTo("Diffuse a tout chauffeur compatible - premier arrive gagne");
        }

        // Les 3 capacites candidates sont bien couvertes (une proposition
        // chacune), quelle que soit leur position d'entree.
        assertThat(propositions).extracting(PropositionEmiseEvent::capaciteId)
                .containsExactlyInAnyOrder(capaciteMoinsChere, capaciteIntermediaire, capacitePlusChere);

        // Le montant diffuse est le vrai prix tarife (meme axe/type/poids pour
        // les 3 candidats de ce test -> meme prix).
        for (PropositionEmiseEvent proposition : propositions) {
            assertThat(proposition.prixTransport()).isEqualByComparingTo("10000");
        }
    }

    @Test
    void fewer_than_two_alternatives_yield_fewer_than_three_propositions() {
        UUID demandeId = UUID.randomUUID();
        UUID axeId = UUID.randomUUID();
        UUID seuleCapacite = UUID.randomUUID();

        List<CandidatCoutDto> candidats = List.of(
                new CandidatCoutDto(seuleCapacite, null, null, null, null, null, "FOURGON"));

        DemandeAvecCandidats demande = new DemandeAvecCandidats(demandeId,
                new PointGeoDto(4.05, 9.7), new PointGeoDto(3.87, 11.52), axeId, BigDecimal.valueOf(1000), candidats, null, null, null, null, null, null, null, null);

        when(serviceMatClient.calculerCoutsLot(any())).thenReturn(new CoutLotResponseDto(demandeId, 1, false,
                List.of(new CoutResponseDto(seuleCapacite, UUID.randomUUID(), BigDecimal.valueOf(10000)))));

        when(affectationRepository.save(any(Affectation.class))).thenAnswer(inv -> {
            Affectation a = inv.getArgument(0);
            assignerId(a, UUID.randomUUID());
            return a;
        });

        when(tarificationL4Service.calculer(eq(axeId), eq("FOURGON"), any(), any(), any()))
                .thenReturn(new TarificationResultat(null, null, "FORFAITAIRE", BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(10000), false,
                        BigDecimal.valueOf(10000), BigDecimal.valueOf(1000), BigDecimal.valueOf(9000), false));

        service.calculerAffectationOptimale(List.of(demande));

        // Un seul candidat disponible -> une seule proposition (rang 1),
        // jamais 3 forcees artificiellement (RG-039 : "au plus trois").
        verify(eventPublisher, times(1)).publierPropositionEmise(any());
    }

    @Test
    void proposition_diffusee_chauffeur_porte_le_transporteur_et_affectation_denormalisee() {
        UUID demandeId = UUID.randomUUID();
        UUID axeId = UUID.randomUUID();
        UUID capacite = UUID.randomUUID();
        UUID transporteur = UUID.randomUUID();

        // Candidat avec transporteurId renseigne (le cas reel cote Moteur,
        // contrairement aux autres tests qui mettent null).
        List<CandidatCoutDto> candidats = List.of(
                new CandidatCoutDto(capacite, transporteur, null, null, null, null, "FOURGON"));

        DemandeAvecCandidats demande = new DemandeAvecCandidats(demandeId,
                new PointGeoDto(4.05, 9.7), new PointGeoDto(3.87, 11.52), axeId,
                BigDecimal.valueOf(1000), candidats,
                null, null, null, null, null, null, null, null);

        when(serviceMatClient.calculerCoutsLot(any())).thenReturn(new CoutLotResponseDto(demandeId, 1, false,
                List.of(new CoutResponseDto(capacite, UUID.randomUUID(), BigDecimal.valueOf(10000)))));

        ArgumentCaptor<Affectation> affectationCaptor = ArgumentCaptor.forClass(Affectation.class);
        when(affectationRepository.save(affectationCaptor.capture())).thenAnswer(inv -> {
            Affectation a = inv.getArgument(0);
            assignerId(a, UUID.randomUUID());
            return a;
        });

        when(tarificationL4Service.calculer(eq(axeId), eq("FOURGON"), any(), any(), any()))
                .thenReturn(new TarificationResultat(null, null, "FORFAITAIRE", BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(10000), false,
                        BigDecimal.valueOf(10000), BigDecimal.valueOf(1000), BigDecimal.valueOf(9000), false));

        service.calculerAffectationOptimale(List.of(demande));

        // L'Affectation persistee denormalise le transporteur (V27) : c'est ce
        // qui permet au GET /proposees de filtrer par transporteur.
        assertThat(affectationCaptor.getValue().getTransporteurId()).isEqualTo(transporteur);

        // Et l'evenement chauffeur est bien publie avec le transporteur cible.
        ArgumentCaptor<PropositionDiffuseeChauffeurEvent> evenementCaptor =
                ArgumentCaptor.forClass(PropositionDiffuseeChauffeurEvent.class);
        verify(eventPublisher, times(1)).publierPropositionDiffuseeChauffeur(evenementCaptor.capture());

        PropositionDiffuseeChauffeurEvent evenement = evenementCaptor.getValue();
        assertThat(evenement.transporteurId()).isEqualTo(transporteur);
        assertThat(evenement.capaciteId()).isEqualTo(capacite);
        assertThat(evenement.demandeId()).isEqualTo(demandeId);
        assertThat(evenement.affectationId()).isNotNull();
        assertThat(evenement.affectationId()).isEqualTo(affectationCaptor.getValue().getId());
        assertThat(evenement.prixTransport()).isEqualByComparingTo("10000");
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
