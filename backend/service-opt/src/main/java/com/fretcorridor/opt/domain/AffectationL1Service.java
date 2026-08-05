package com.fretcorridor.opt.domain;

import com.fretcorridor.opt.algorithm.KuhnMunkresSolver;
import com.fretcorridor.opt.client.CandidatCoutDto;
import com.fretcorridor.opt.client.CoutLotRequestDto;
import com.fretcorridor.opt.client.CoutLotResponseDto;
import com.fretcorridor.opt.client.CoutResponseDto;
import com.fretcorridor.opt.client.ItineraireRequestDto;
import com.fretcorridor.opt.client.ItineraireResponseDto;
import com.fretcorridor.opt.client.ServiceMatClient;
import com.fretcorridor.opt.client.ValhallaClient;
import com.fretcorridor.opt.messaging.AffectationConfirmeeEvent;
import com.fretcorridor.opt.messaging.OptEventPublisher;
import com.fretcorridor.opt.messaging.PropositionEmiseEvent;
import com.fretcorridor.opt.tarification.TarificationL4Service;
import com.fretcorridor.opt.tarification.TarificationResultat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Orchestration du L1 (Sprint 5, EF-MAT-01/02/03) : appariement optimal par
 * lots via Kuhn-Munkres, JAMAIS glouton (anti-patron explicite du CDC).
 *
 * Construit la matrice de cout demande x capacite en appelant service-mat une
 * fois par demande (coherent avec le contrat CoutLotRequest cote MAT : cout
 * d'un lot de candidats face a UNE demande), puis resout l'affectation
 * optimale sur l'ensemble du lot d'un coup.
 *
 * Toutes les demandes du lot DOIVENT partager exactement le meme ensemble de
 * candidats - coherent avec le fait qu'elles proviennent du meme filtrage L0
 * (meme zone H3) : sinon une colonne "capacite" ne designerait pas la meme
 * capacite sur toutes les lignes et la matrice n'aurait pas de sens.
 */
@Service
public class AffectationL1Service {

    private static final Logger log = LoggerFactory.getLogger(AffectationL1Service.class);

    private final ServiceMatClient serviceMatClient;
    private final ValhallaClient valhallaClient;
    private final TarificationL4Service tarificationL4Service;
    private final AffectationRepository affectationRepository;
    private final OptEventPublisher eventPublisher;

    public AffectationL1Service(ServiceMatClient serviceMatClient, ValhallaClient valhallaClient,
                                 TarificationL4Service tarificationL4Service,
                                 AffectationRepository affectationRepository,
                                 OptEventPublisher eventPublisher) {
        this.serviceMatClient = serviceMatClient;
        this.valhallaClient = valhallaClient;
        this.tarificationL4Service = tarificationL4Service;
        this.affectationRepository = affectationRepository;
        this.eventPublisher = eventPublisher;
    }

    public AffectationLotResultat calculerAffectationOptimale(List<DemandeAvecCandidats> demandes) {
        if (demandes == null || demandes.isEmpty()) {
            return new AffectationLotResultat(false, List.of());
        }

        List<UUID> capacitesReference = demandes.get(0).candidats().stream()
                .map(CandidatCoutDto::capaciteId)
                .toList();
        Set<UUID> capacitesReferenceSet = new LinkedHashSet<>(capacitesReference);

        for (DemandeAvecCandidats demande : demandes) {
            Set<UUID> capacitesDemande = new LinkedHashSet<>();
            demande.candidats().forEach(c -> capacitesDemande.add(c.capaciteId()));
            if (!capacitesDemande.equals(capacitesReferenceSet)) {
                throw new IllegalArgumentException(
                        "Toutes les demandes du lot L1 doivent partager exactement le meme "
                                + "ensemble de capacites candidates (issu du meme filtrage L0). "
                                + "Ecart detecte sur la demande " + demande.demandeId());
            }
        }

        int nbDemandes = demandes.size();
        int nbCapacites = capacitesReference.size();
        double[][] matriceCouts = new double[nbDemandes][nbCapacites];
        UUID[][] cycleMatchingIds = new UUID[nbDemandes][nbCapacites];

        for (int i = 0; i < nbDemandes; i++) {
            DemandeAvecCandidats demande = demandes.get(i);

            CoutLotResponseDto reponse = serviceMatClient.calculerCoutsLot(
                    new CoutLotRequestDto(demande.demandeId(), demande.candidats()));

            if (reponse == null) {
                log.warn("service-mat injoignable pour la demande {} - lot L1 en mode degrade, "
                        + "aucune affectation produite.", demande.demandeId());
                return new AffectationLotResultat(true, List.of());
            }

            List<CoutResponseDto> resultats = reponse.resultats();
            for (int j = 0; j < nbCapacites; j++) {
                matriceCouts[i][j] = resultats.get(j).coutTotal().doubleValue();
                cycleMatchingIds[i][j] = resultats.get(j).cycleMatchingId();
            }
        }

        int[] affectationParDemande = KuhnMunkresSolver.resoudre(matriceCouts);

        List<AffectationResultat> resultatsFinaux = new ArrayList<>(nbDemandes);
        for (int i = 0; i < nbDemandes; i++) {
            int indiceCapacite = affectationParDemande[i];
            DemandeAvecCandidats demande = demandes.get(i);
            UUID demandeId = demande.demandeId();

            if (indiceCapacite == -1) {
                resultatsFinaux.add(new AffectationResultat(demandeId, null, null, null, null, null, null));
                continue;
            }

            UUID capaciteId = capacitesReference.get(indiceCapacite);
            CandidatCoutDto candidatRetenu = demande.candidats().get(indiceCapacite);

            ItineraireResponseDto itineraire = calculerItineraireSiPossible(demande, candidatRetenu);

            Double distanceMetres = itineraire != null ? itineraire.distanceMetres() : null;
            TarificationResultat tarification = tarificationL4Service.calculer(
                    demande.axeId(), candidatRetenu.typeVehicule(), demande.poidsTaxableKg(),
                    distanceMetres, BigDecimal.ZERO);

            Affectation affectation = new Affectation(
                    demandeId, capaciteId, cycleMatchingIds[i][indiceCapacite],
                    demande.origineDemande().latitude(), demande.origineDemande().longitude(),
                    demande.destinationDemande().latitude(), demande.destinationDemande().longitude(),
                    itineraire != null ? itineraire.distanceMetres() : null,
                    itineraire != null ? itineraire.dureeSecondes() : null,
                    itineraire != null ? itineraire.intervalleConfianceSecondes() : null,
                    itineraire != null ? itineraire.geometrieEncodee() : null,
                    BigDecimal.valueOf(matriceCouts[i][indiceCapacite]),
                    tarification.baremeId(), tarification.baremeVersion(), tarification.regime(),
                    tarification.coutBase(), tarification.coutVariablePoidsTaxable(),
                    tarification.coutServices(), tarification.facteurTensionApplique(),
                    tarification.prixTransportAvantPlancher(), tarification.plancherApplique(),
                    tarification.prixTransport(), tarification.commissionPlateforme(),
                    tarification.montantVerseTransporteur(), tarification.modeDegrade());
            UUID missionId = affectationRepository.save(affectation).getId();

            resultatsFinaux.add(new AffectationResultat(
                    demandeId,
                    capaciteId,
                    missionId,
                    BigDecimal.valueOf(matriceCouts[i][indiceCapacite]),
                    cycleMatchingIds[i][indiceCapacite],
                    itineraire,
                    tarification));

            // --- Publication Kafka : PropositionEmise (→ service-mkt) ---
            if (missionId != null && tarification != null) {
                PropositionEmiseEvent proposition = new PropositionEmiseEvent(
                        UUID.randomUUID(),
                        cycleMatchingIds[i][indiceCapacite],
                        demandeId,
                        capaciteId,
                        missionId,
                        demande.axeId(),
                        1, // rang 1 pour l'affectation optimale
                        "Affectation optimale L1 (Kuhn-Munkres)",
                        tarification.prixTransport(),
                        tarification.commissionPlateforme(),
                        "XAF",
                        itineraire != null ? itineraire.distanceMetres() : 0,
                        itineraire != null ? itineraire.dureeSecondes() : null,
                        demande.origineDemande() != null ? "Origine" : null,
                        demande.destinationDemande() != null ? "Destination" : null,
                        Instant.now()
                );
                eventPublisher.publierPropositionEmise(proposition);

                // --- Publication Kafka : AffectationConfirmee (→ service-exe) ---
                AffectationConfirmeeEvent confirmation = new AffectationConfirmeeEvent(
                        UUID.randomUUID(),
                        missionId,
                        demandeId,
                        capaciteId,
                        candidatRetenu.vehiculeId(),
                        candidatRetenu.transporteurId(),
                        demande.chargeurId(),
                        demande.axeId(),
                        demande.origineDemande() != null ? demande.origineDemande().latitude() : 0,
                        demande.origineDemande() != null ? demande.origineDemande().longitude() : 0,
                        null,
                        demande.destinationDemande() != null ? demande.destinationDemande().latitude() : 0,
                        demande.destinationDemande() != null ? demande.destinationDemande().longitude() : 0,
                        null,
                        itineraire != null ? itineraire.distanceMetres() : null,
                        itineraire != null ? itineraire.dureeSecondes() : null,
                        itineraire != null ? itineraire.intervalleConfianceSecondes() : null,
                        itineraire != null ? itineraire.geometrieEncodee() : null,
                        tarification.prixTransport(),
                        tarification.commissionPlateforme(),
                        tarification.montantVerseTransporteur(),
                        "XAF",
                        candidatRetenu.modeCollecte(),
                        candidatRetenu.modeRemise(),
                        Instant.now()
                );
                eventPublisher.publierAffectationConfirmee(confirmation);
            }
        }

        return new AffectationLotResultat(false, resultatsFinaux);
    }

    private ItineraireResponseDto calculerItineraireSiPossible(DemandeAvecCandidats demande,
                                                                 CandidatCoutDto candidatRetenu) {
        if (candidatRetenu.positionCapacite() == null
                || demande.origineDemande() == null
                || demande.destinationDemande() == null) {
            log.warn("Coordonnees incompletes pour la demande {} / capacite {} - "
                            + "itineraire Valhalla non calcule (mode degrade sur ce candidat uniquement).",
                    demande.demandeId(), candidatRetenu.capaciteId());
            return null;
        }

        ItineraireRequestDto requete = new ItineraireRequestDto(
                List.of(candidatRetenu.positionCapacite(), demande.origineDemande(), demande.destinationDemande()),
                candidatRetenu.profilCamion());

        return valhallaClient.calculerItineraire(requete);
    }
}
