package com.fretcorridor.opt.domain;

import com.fretcorridor.opt.algorithm.KuhnMunkresSolver;
import com.fretcorridor.opt.client.CandidatCoutDto;
import com.fretcorridor.opt.client.CoutLotRequestDto;
import com.fretcorridor.opt.client.CoutLotResponseDto;
import com.fretcorridor.opt.client.CoutResponseDto;
import com.fretcorridor.opt.client.ItineraireRequestDto;
import com.fretcorridor.opt.client.ItineraireResponseDto;
import com.fretcorridor.opt.client.AxeDetailDto;
import com.fretcorridor.opt.client.ServiceCapClient;
import com.fretcorridor.opt.client.ServiceGeoClient;
import com.fretcorridor.opt.client.ServiceMatClient;
import com.fretcorridor.opt.client.ServiceNotClient;
import com.fretcorridor.opt.client.ValhallaClient;
import com.fretcorridor.opt.messaging.AffectationConfirmeeEvent;
import com.fretcorridor.opt.messaging.OptEventPublisher;
import com.fretcorridor.opt.messaging.PropositionEmiseEvent;
import com.fretcorridor.opt.messaging.RepartitionConventionnelleAppliqueeEvent;
import com.fretcorridor.opt.tarification.TarificationL4Service;
import com.fretcorridor.opt.tarification.TarificationResultat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
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

    // UC-MAT-02 (CDC) : duree avant expiration silencieuse d'une proposition
    // non traitee (flux d'exception E1). Le CDC ne fixe pas de duree precise,
    // seulement "un compte a rebours avant expiration" (RG-050 : 3
    // interactions au plus, pas une contrainte de temps) -- 15 minutes est un
    // choix d'implementation raisonnable pour ce MVP, pas une valeur du CDC.
    static final Duration DUREE_VALIDITE_PROPOSITION = Duration.ofMinutes(15);

    private final ServiceMatClient serviceMatClient;
    private final ValhallaClient valhallaClient;
    private final TarificationL4Service tarificationL4Service;
    private final AffectationRepository affectationRepository;
    private final PropositionMissionRepository propositionMissionRepository;
    private final OptEventPublisher eventPublisher;
    private final ServiceGeoClient serviceGeoClient;
    private final ServiceCapClient serviceCapClient;
    private final ServiceNotClient serviceNotClient;

    public AffectationL1Service(ServiceMatClient serviceMatClient, ValhallaClient valhallaClient,
                                 TarificationL4Service tarificationL4Service,
                                 AffectationRepository affectationRepository,
                                 PropositionMissionRepository propositionMissionRepository,
                                 OptEventPublisher eventPublisher,
                                 ServiceGeoClient serviceGeoClient,
                                 ServiceCapClient serviceCapClient,
                                 ServiceNotClient serviceNotClient) {
        this.serviceMatClient = serviceMatClient;
        this.valhallaClient = valhallaClient;
        this.tarificationL4Service = tarificationL4Service;
        this.affectationRepository = affectationRepository;
        this.propositionMissionRepository = propositionMissionRepository;
        this.eventPublisher = eventPublisher;
        this.serviceGeoClient = serviceGeoClient;
        this.serviceCapClient = serviceCapClient;
        this.serviceNotClient = serviceNotClient;
    }

    public AffectationLotResultat calculerAffectationOptimale(List<DemandeAvecCandidats> demandes) {
        return calculerAffectationOptimale(demandes, null, null);
    }

    // BUG CORRIGE (audit de suivi Mobile) : origineNom/destinationNom
    // partaient toujours en dur a null dans AffectationConfirmeeEvent plus
    // bas -- Mission.origineNom/destinationNom (service-exe) restaient donc
    // systematiquement vides, et l'app Chauffeur (qui affiche pourtant deja
    // ces deux champs sur la liste des missions et le detail) ne montrait
    // jamais l'axe. Surcharge plutot que modifier la signature existante :
    // AffectationL1Controller (endpoint de verification manuelle, Sprint 5)
    // et les tests existants n'ont pas de nom d'axe a fournir.
    public AffectationLotResultat calculerAffectationOptimale(List<DemandeAvecCandidats> demandes,
                                                                String origineNom, String destinationNom) {
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
                    new CoutLotRequestDto(demande.demandeId(), demande.axeId(), demande.candidats()));

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

            // Affectation.origineLatitude/destinationLatitude sont des
            // primitives double non-nullables (colonnes NOT NULL) -- une
            // demande publiee sans coordonnees (donnee de test incomplete,
            // geocodage jamais branche a la publication) ne peut pas y etre
            // persistee sans fabriquer une position inventee. On la laisse
            // non affectee ce cycle plutot que de planter tout le lot
            // (BUG CORRIGE : plantait ici auparavant, bloquant aussi les
            // autres demandes valides du meme axe).
            if (demande.origineDemande() == null || demande.destinationDemande() == null) {
                log.warn("Demande {} sans coordonnees origine/destination - non affectee ce cycle "
                        + "(mode degrade, cf. javadoc).", demande.demandeId());
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

            resultatsFinaux.add(new AffectationResultat(
                    demandeId,
                    capaciteId,
                    null, // UC-MAT-02 : plus de missionId ici -- voir plus bas
                    BigDecimal.valueOf(matriceCouts[i][indiceCapacite]),
                    cycleMatchingIds[i][indiceCapacite],
                    itineraire,
                    tarification));

            // UC-MAT-02 du CDC (page 43, "Notification, acceptation ou refus
            // d'une mission par le chauffeur") : BUG CORRIGE (26/08) -- le
            // rang 1 etait jusqu'ici auto-confirme (Affectation creee,
            // capacite reservee, Mission publiee) SANS jamais demander
            // l'accord du chauffeur/transporteur, contrairement au flux
            // explicitement decrit par le CDC (notification -> remuneration
            // affichee en premier RG-049 -> accepter/refuser en 3
            // interactions au plus RG-050 -> reservation SEULEMENT a
            // l'acceptation). Cree desormais une PropositionMission
            // EN_ATTENTE ; voir PropositionMissionService.accepter() pour la
            // suite du flux (ex-contenu de ce bloc : Affectation,
            // AffectationConfirmee, reservation capacite, repartition
            // conventionnelle -- tout deplace la, inchange sur le fond,
            // simplement declenche a l'acceptation plutot qu'ici).
            if (tarification != null) {
                PropositionMission proposition = new PropositionMission(
                        demandeId, capaciteId, candidatRetenu.transporteurId(), candidatRetenu.vehiculeId(),
                        candidatRetenu.typeVehicule(), cycleMatchingIds[i][indiceCapacite], demande.axeId(), 1,
                        demande.poidsTaxableKg(), origineNom, destinationNom,
                        demande.origineDemande().latitude(), demande.origineDemande().longitude(),
                        demande.destinationDemande().latitude(), demande.destinationDemande().longitude(),
                        itineraire != null ? itineraire.distanceMetres() : null,
                        itineraire != null ? itineraire.dureeSecondes() : null,
                        itineraire != null ? itineraire.intervalleConfianceSecondes() : null,
                        itineraire != null ? itineraire.geometrieEncodee() : null,
                        tarification.prixTransport(), BigDecimal.valueOf(matriceCouts[i][indiceCapacite]),
                        demande.typeEmballageNom(), demande.quantite(),
                        demande.destinataireNom(), demande.destinataireTelephone(),
                        demande.modeCollecte(), demande.typeDisponibilite(),
                        demande.poidsTotalKg(), demande.grandeValeur(),
                        Instant.now().plus(DUREE_VALIDITE_PROPOSITION));

                if (candidatRetenu.transporteurId() == null) {
                    // Degradation gracieuse (ENF-DIS-04) : sans transporteur
                    // resolu, personne a notifier -- la demande reste
                    // consideree "traitee" ce cycle (comportement inchange,
                    // cf MatchingCycleService) mais aucune proposition n'est
                    // creee ; sera reconsideree comme n'importe quelle
                    // demande non affectee. Log explicite : ce cas ne devrait
                    // plus arriver depuis le fix S7 (transporteurId toujours
                    // resolu par CandidatCoutDto).
                    log.warn("Candidat retenu sans transporteurId pour la demande {} - "
                            + "aucune PropositionMission creee (UC-MAT-02 impossible sans destinataire).", demandeId);
                } else {
                    propositionMissionRepository.save(proposition);

                    serviceNotClient.notifier(
                            candidatRetenu.transporteurId(),
                            "Nouvelle mission proposee",
                            "%s → %s, %s".formatted(
                                    origineNom != null ? origineNom : "Origine",
                                    destinationNom != null ? destinationNom : "Destination",
                                    tarification.prixTransport() != null
                                            ? "%s XAF".formatted(tarification.prixTransport().toBigInteger())
                                            : "prix a confirmer"),
                            "PROPOSITION_MISSION",
                            proposition.getId(),
                            null);
                }

                // RG-039/EF-MKT-07 (CDC : "au plus trois propositions par
                // demande, ordonnées, motif de classement intelligible") :
                // rang 2/3 restent des alternatives purement
                // informationnelles cote chargeur (aucune interaction
                // chauffeur, hors perimetre UC-MAT-02 pour cette iteration) --
                // comportement inchange.
                publierAlternatives(demandeId, demande, matriceCouts[i], capacitesReference,
                        cycleMatchingIds[i], indiceCapacite);
            }
        }

        return new AffectationLotResultat(false, resultatsFinaux);
    }

    /**
     * UC-MAT-02 (CDC) : suite du flux, declenchee par
     * PropositionMissionService.accepter() -- reprend exactement ce que
     * faisait AffectationL1Service au sortir du solveur avant le 26/08
     * (Affectation, AffectationConfirmee, reservation capacite, repartition
     * conventionnelle), a partir des donnees deja capturees sur la
     * proposition acceptee. Tarification recalculee (deterministe a partir
     * de axeId/typeVehicule/poidsTaxableKg/distanceMetres deja stockes)
     * plutot que dupliquee sur PropositionMission -- voir javadoc de la
     * migration V22.
     */
    Affectation confirmerDepuisProposition(PropositionMission proposition) {
        TarificationResultat tarification = tarificationL4Service.calculer(
                proposition.getAxeId(), proposition.getTypeVehicule(), proposition.getPoidsTaxableKg(),
                proposition.getDistanceMetres(), BigDecimal.ZERO);

        Affectation affectation = new Affectation(
                proposition.getDemandeId(), proposition.getCapaciteId(), proposition.getCycleMatchingId(),
                proposition.getAxeId(), proposition.getPoidsTaxableKg(),
                proposition.getOrigineLatitude(), proposition.getOrigineLongitude(),
                proposition.getDestinationLatitude(), proposition.getDestinationLongitude(),
                proposition.getDistanceMetres(), proposition.getDureeSecondes(),
                proposition.getIntervalleConfianceSecondes(), proposition.getGeometrieEncodee(),
                proposition.getCoutTotal(),
                tarification.baremeId(), tarification.baremeVersion(), tarification.regime(),
                tarification.coutBase(), tarification.coutVariablePoidsTaxable(),
                tarification.coutServices(), tarification.facteurTensionApplique(),
                tarification.prixTransportAvantPlancher(), tarification.plancherApplique(),
                tarification.prixTransport(), tarification.commissionPlateforme(),
                tarification.montantVerseTransporteur(), tarification.modeDegrade());
        UUID missionId = affectationRepository.save(affectation).getId();

        PropositionEmiseEvent propositionEvent = new PropositionEmiseEvent(
                UUID.randomUUID(),
                proposition.getCycleMatchingId(),
                proposition.getDemandeId(),
                proposition.getCapaciteId(),
                missionId,
                proposition.getAxeId(),
                1,
                "Affectation optimale L1 (Kuhn-Munkres)",
                tarification.prixTransport(),
                tarification.commissionPlateforme(),
                "XAF",
                proposition.getDistanceMetres() != null ? proposition.getDistanceMetres() : 0,
                proposition.getDureeSecondes() != null ? proposition.getDureeSecondes().longValue() : null,
                proposition.getOrigineNom() != null ? "Origine" : null,
                proposition.getDestinationNom() != null ? "Destination" : null,
                Instant.now());
        eventPublisher.publierPropositionEmise(propositionEvent);

        AffectationConfirmeeEvent confirmation = new AffectationConfirmeeEvent(
                UUID.randomUUID(),
                missionId,
                proposition.getDemandeId(),
                proposition.getCapaciteId(),
                proposition.getVehiculeId(),
                proposition.getTransporteurId(),
                null,
                proposition.getAxeId(),
                proposition.getOrigineLatitude(),
                proposition.getOrigineLongitude(),
                proposition.getOrigineNom(),
                proposition.getDestinationLatitude(),
                proposition.getDestinationLongitude(),
                proposition.getDestinationNom(),
                proposition.getDistanceMetres(),
                proposition.getDureeSecondes() != null ? proposition.getDureeSecondes().longValue() : null,
                proposition.getIntervalleConfianceSecondes() != null
                        ? proposition.getIntervalleConfianceSecondes().longValue() : null,
                proposition.getGeometrieEncodee(),
                tarification.prixTransport(),
                tarification.commissionPlateforme(),
                tarification.montantVerseTransporteur(),
                "XAF",
                "DEPOT",
                "RETRAIT",
                Instant.now(),
                proposition.getTypeEmballageNom(),
                proposition.getQuantite(),
                proposition.getPoidsTaxableKg(),
                proposition.getDestinataireNom(),
                proposition.getDestinataireTelephone(),
                proposition.getModeCollecte(),
                proposition.getTypeDisponibilite(),
                proposition.getPoidsTotalKg(),
                proposition.getGrandeValeur());
        eventPublisher.publierAffectationConfirmee(confirmation);

        // BUG CORRIGE (audit de suivi, 23 aout) : reservation reelle de la
        // capacite cote service-cap -- voir javadoc ServiceCapClient.
        if (proposition.getPoidsTaxableKg() != null) {
            serviceCapClient.reserver(proposition.getCapaciteId(), proposition.getPoidsTaxableKg(), missionId.toString());
        }

        // --- Publication Kafka conditionnelle : RepartitionConventionnelleAppliquee
        // (-> service-pay, EF-GEO-05/RG-052, Phase 4). Voir commentaire original
        // (avant deplacement ici) pour le detail de la logique et sa limite connue.
        if (proposition.getAxeId() != null) {
            AxeDetailDto axeDetail = serviceGeoClient.axeParId(proposition.getAxeId());
            if (axeDetail != null && axeDetail.parametres() != null
                    && axeDetail.parametres().get("conventionRepartition") instanceof java.util.Map<?, ?> conventionMap) {

                Object conventionCodeObj = conventionMap.get("conventionCode");
                Object partsObj = conventionMap.get("partsPourcent");

                if (conventionCodeObj != null && partsObj instanceof java.util.Map<?, ?> partsMap) {
                    java.util.Map<String, Double> parts = new java.util.HashMap<>();
                    for (var entree : partsMap.entrySet()) {
                        if (entree.getValue() instanceof Number n) {
                            parts.put(entree.getKey().toString(), n.doubleValue());
                        }
                    }

                    RepartitionConventionnelleAppliqueeEvent repartition = new RepartitionConventionnelleAppliqueeEvent(
                            UUID.randomUUID(),
                            missionId,
                            proposition.getAxeId(),
                            conventionCodeObj.toString(),
                            parts,
                            Instant.now(),
                            false);
                    eventPublisher.publierRepartitionConventionnelleAppliquee(repartition);
                }
            }
        }

        return affectation;
    }

    /**
     * RG-039/EF-MKT-07 : jusqu'à 2 alternatives supplémentaires (rang 2/3),
     * classées par coût composite croissant parmi les candidats non retenus
     * de cette demande (le coût composite sert uniquement à les CLASSER,
     * jamais à les CHIFFRER). Purement informationnelles -- aucune
     * Affectation créée, prix estimé (pas ferme, RG-041) recalculé via
     * TarificationL4Service sans itinéraire Valhalla (distanceMetres=null)
     * pour limiter le surcoût réseau sur des candidats qui peuvent ne jamais
     * être acceptés -- un régime FORFAITAIRE_VEHICULE produit quand même un
     * vrai prix sans distance ; seul un régime POIDS_TAXABLE repasse alors en
     * mode dégradé, auquel cas l'alternative est omise plutôt que de publier
     * un prix inventé (ENF-DIS-04).
     *
     * BUG CORRIGE (26/08) : publiait auparavant BigDecimal.valueOf(cout) --
     * le score composite [0,1] pondéré (service-mat) lui-même, jamais un prix
     * en XAF -- d'où des propositions "2e/3e meilleur prix" affichées à ~2.2
     * XAF côté app Client à côté d'un rang 1 à plusieurs dizaines de milliers
     * de XAF.
     *
     * missionId volontairement null -- distingue une vraie affectation
     * committée (rang 1) d'une simple alternative (cf Proposition.missionId,
     * service-mkt, colonne nullable).
     */
    private void publierAlternatives(UUID demandeId, DemandeAvecCandidats demande, double[] coutsLigne,
                                      List<UUID> capacitesReference, UUID[] cycleMatchingIdsLigne, int indiceRetenu) {
        record CandidatAlternatif(int indice, double cout) {
        }
        List<CandidatAlternatif> autres = new ArrayList<>();
        for (int j = 0; j < coutsLigne.length; j++) {
            // FIX (audit CDC 20/08) : exclut les cases sentinelles - une
            // capacite hors du rayon RG-046 de CETTE demande ne doit jamais
            // etre presentee comme "2e/3e meilleur prix", meme si son cout
            // brut trie plus bas que d'autres sentinelles.
            if (j != indiceRetenu && coutsLigne[j] < KuhnMunkresSolver.COUT_SENTINELLE) {
                autres.add(new CandidatAlternatif(j, coutsLigne[j]));
            }
        }
        autres.sort(Comparator.comparingDouble(CandidatAlternatif::cout));

        int rang = 2;
        for (CandidatAlternatif candidat : autres.stream().limit(2).toList()) {
            CandidatCoutDto candidatDto = demande.candidats().get(candidat.indice());
            TarificationResultat tarification = tarificationL4Service.calculer(
                    demande.axeId(), candidatDto.typeVehicule(), demande.poidsTaxableKg(), null, BigDecimal.ZERO);
            if (tarification.modeDegrade()) {
                // Pas de barème forfaitaire applicable sans distance -- on
                // n'invente jamais de prix (ENF-DIS-04), cette alternative
                // est simplement omise plutôt que d'afficher un prix bidon.
                rang++;
                continue;
            }

            PropositionEmiseEvent alternative = new PropositionEmiseEvent(
                    UUID.randomUUID(),
                    cycleMatchingIdsLigne[candidat.indice()],
                    demandeId,
                    capacitesReference.get(candidat.indice()),
                    null,
                    demande.axeId(),
                    rang,
                    rang == 2 ? "2e meilleur prix" : "3e meilleur prix",
                    tarification.prixTransport(),
                    tarification.commissionPlateforme(),
                    "XAF",
                    0,
                    null,
                    demande.origineDemande() != null ? "Origine" : null,
                    demande.destinationDemande() != null ? "Destination" : null,
                    Instant.now());
            eventPublisher.publierPropositionEmise(alternative);
            rang++;
        }
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
