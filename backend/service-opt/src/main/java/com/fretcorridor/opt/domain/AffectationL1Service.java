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
import com.fretcorridor.opt.tarification.TarificationL4Service;
import com.fretcorridor.opt.tarification.TarificationResultat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
    // Persiste l'affectation confirmee : comble le trou d'architecture identifie -
    // c'est la source de verite que TRK consultera en synchrone interne (meme
    // porteur) pour connaitre origine/destination d'une mission et calculer son ETA.
    private final AffectationRepository affectationRepository;

    public AffectationL1Service(ServiceMatClient serviceMatClient, ValhallaClient valhallaClient,
                                 TarificationL4Service tarificationL4Service,
                                 AffectationRepository affectationRepository) {
        this.serviceMatClient = serviceMatClient;
        this.valhallaClient = valhallaClient;
        this.tarificationL4Service = tarificationL4Service;
        this.affectationRepository = affectationRepository;
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
                // Pas d'affectation ce cycle (plus de demandes que de capacites) -
                // pas d'itineraire a calculer, pas de tarification non plus :
                // ce n'est pas un mode degrade, juste l'absence d'affectation.
                resultatsFinaux.add(new AffectationResultat(demandeId, null, null, null, null, null, null));
                continue;
            }

            UUID capaciteId = capacitesReference.get(indiceCapacite);
            CandidatCoutDto candidatRetenu = demande.candidats().get(indiceCapacite);

            // Etape 3 du moteur V0 (README module optimisation) : appel Valhalla
            // pour le calcul de trajet, une fois l'affectation optimale connue -
            // jamais avant, pour ne pas calculer d'itineraires inutiles sur des
            // candidats finalement non retenus (le budget Valhalla, cf ADR dans
            // ValhallaClientProperties, est nettement plus genereux que L0/L1 mais
            // reste couteux a l'echelle d'un lot entier).
            ItineraireResponseDto itineraire = calculerItineraireSiPossible(demande, candidatRetenu);

            // Etape 4 du moteur V0 (Tarification L4, CDC S8.9) : la distance
            // vient de Valhalla si l'itineraire a pu etre calcule, null sinon
            // (regime FORFAITAIRE_VEHICULE n'en a pas besoin - cf
            // TarificationL4Service). facteurTensionBrut = ZERO explicite :
            // l'observatoire de marche (EF-BUR, Phase 3) n'alimente pas
            // encore ce signal en V0 - tension neutre plutot qu'un chiffre
            // invente, jamais l'inverse.
            Double distanceMetres = itineraire != null ? itineraire.distanceMetres() : null;
            TarificationResultat tarification = tarificationL4Service.calculer(
                    demande.axeId(), candidatRetenu.typeVehicule(), demande.poidsTaxableKg(),
                    distanceMetres, BigDecimal.ZERO);

            // Persistance de l'affectation confirmee (comble le trou d'architecture) :
            // toutes les coordonnees itineraire/tarification sont extraites ici en
            // valeurs nullables individuelles - chaque champ peut etre en mode
            // degrade independamment (cf javadoc Affectation), jamais tout ou rien.
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
        }

        return new AffectationLotResultat(false, resultatsFinaux);
    }

    /**
     * Construit la requete Valhalla (position de la capacite -> origine demande
     * -> destination demande) et delegue a ValhallaClient. Retourne null sans
     * lancer d'exception si une coordonnee manque (candidat/demande incomplets,
     * cf javadoc ProfilCamionDto sur la faible completude des donnees ouvertes
     * africaines) ou si Valhalla echoue - degradation gracieuse en cascade,
     * jamais de blocage du cycle L1 pour une seule affectation (ENF-DIS-04).
     */
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
