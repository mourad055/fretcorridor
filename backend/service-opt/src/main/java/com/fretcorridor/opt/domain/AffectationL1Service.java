package com.fretcorridor.opt.domain;

import com.fretcorridor.opt.algorithm.KuhnMunkresSolver;
import com.fretcorridor.opt.client.CandidatCoutDto;
import com.fretcorridor.opt.client.CoutLotRequestDto;
import com.fretcorridor.opt.client.CoutLotResponseDto;
import com.fretcorridor.opt.client.CoutResponseDto;
import com.fretcorridor.opt.client.ItineraireRequestDto;
import com.fretcorridor.opt.client.ItineraireResponseDto;
import com.fretcorridor.opt.client.AxeDetailDto;
import com.fretcorridor.opt.client.ServiceGeoClient;
import com.fretcorridor.opt.client.ServiceMatClient;
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

    private final ServiceMatClient serviceMatClient;
    private final ValhallaClient valhallaClient;
    private final TarificationL4Service tarificationL4Service;
    private final AffectationRepository affectationRepository;
    private final OptEventPublisher eventPublisher;
    private final ServiceGeoClient serviceGeoClient;

    public AffectationL1Service(ServiceMatClient serviceMatClient, ValhallaClient valhallaClient,
                                 TarificationL4Service tarificationL4Service,
                                 AffectationRepository affectationRepository,
                                 OptEventPublisher eventPublisher,
                                 ServiceGeoClient serviceGeoClient) {
        this.serviceMatClient = serviceMatClient;
        this.valhallaClient = valhallaClient;
        this.tarificationL4Service = tarificationL4Service;
        this.affectationRepository = affectationRepository;
        this.eventPublisher = eventPublisher;
        this.serviceGeoClient = serviceGeoClient;
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

            Affectation affectation = new Affectation(
                    demandeId, capaciteId, cycleMatchingIds[i][indiceCapacite], demande.axeId(),
                    demande.poidsTaxableKg(),
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
                        itineraire != null ? (long) itineraire.dureeSecondes() : null,
                        demande.origineDemande() != null ? "Origine" : null,
                        demande.destinationDemande() != null ? "Destination" : null,
                        Instant.now()
                );
                eventPublisher.publierPropositionEmise(proposition);

                // RG-039/EF-MKT-07 (CDC : "au plus trois propositions par
                // demande, ordonnées, motif de classement intelligible") :
                // rang 1 ci-dessus reste l'affectation Kuhn-Munkres committee
                // (comportement inchangé) ; rang 2/3 sont des alternatives
                // purement informationnelles, classees par cout sur la meme
                // ligne de la matrice -- aucune Affectation/AffectationConfirmee
                // pour elles, prix estime (pas ferme au sens RG-041 tant que
                // non accepte explicitement, cf AccepterPropositionService).
                publierAlternatives(demandeId, demande, matriceCouts[i], capacitesReference,
                        cycleMatchingIds[i], indiceCapacite);

                // --- Publication Kafka : AffectationConfirmee (→ service-exe) ---
                AffectationConfirmeeEvent confirmation = new AffectationConfirmeeEvent(
                        UUID.randomUUID(),
                        missionId,
                        demandeId,
                        capaciteId,
                        candidatRetenu.vehiculeId(),
                        candidatRetenu.transporteurId(),
                        null,
                        demande.axeId(),
                        demande.origineDemande() != null ? demande.origineDemande().latitude() : 0,
                        demande.origineDemande() != null ? demande.origineDemande().longitude() : 0,
                        origineNom,
                        demande.destinationDemande() != null ? demande.destinationDemande().latitude() : 0,
                        demande.destinationDemande() != null ? demande.destinationDemande().longitude() : 0,
                        destinationNom,
                        itineraire != null ? itineraire.distanceMetres() : null,
                        itineraire != null ? (long) itineraire.dureeSecondes() : null,
                        itineraire != null ? (long) itineraire.intervalleConfianceSecondes() : null,
                        itineraire != null ? itineraire.geometrieEncodee() : null,
                        tarification.prixTransport(),
                        tarification.commissionPlateforme(),
                        tarification.montantVerseTransporteur(),
                        "XAF",
                        "DEPOT",
                        "RETRAIT",
                        Instant.now(),
                        demande.typeEmballageNom(),
                        demande.quantite(),
                        demande.poidsTaxableKg()
                );
                eventPublisher.publierAffectationConfirmee(confirmation);

                // --- Publication Kafka conditionnelle : RepartitionConventionnelleAppliquee
                // (-> service-pay, EF-GEO-05/RG-052, Phase 4). Uniquement si l'axe porte une
                // convention configuree (Axe.parametres.conventionRepartition) - RG-052 :
                // "en trafic intra-camerounais, aucune cle ne s'applique", donc l'absence de
                // convention N'EST PAS un mode degrade, c'est le comportement attendu par
                // defaut pour la grande majorite des axes. Limitation connue (README Phase 4
                // S3.3, Hub sans champ pays) : ne distingue pas encore "axe transfrontalier
                // sans convention renseignee" de "axe intra-camerounais normal" - les deux
                // cas aboutissent au meme silence ici, assume jusqu'a l'ajout de Hub.pays.
                if (demande.axeId() != null) {
                    AxeDetailDto axeDetail = serviceGeoClient.axeParId(demande.axeId());
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
                                    demande.axeId(),
                                    conventionCodeObj.toString(),
                                    parts,
                                    Instant.now(),
                                    false);
                            eventPublisher.publierRepartitionConventionnelleAppliquee(repartition);
                        }
                    }
                }
            }
        }

        return new AffectationLotResultat(false, resultatsFinaux);
    }

    /**
     * RG-039/EF-MKT-07 : jusqu'à 2 alternatives supplémentaires (rang 2/3),
     * classées par coût croissant parmi les candidats non retenus de cette
     * demande. Purement informationnelles -- aucune Affectation créée, prix
     * estimé (pas ferme, RG-041) directement dérivé du coût composite
     * (service-mat), sans recalcul tarification/itinéraire Valhalla pour
     * limiter le surcoût réseau sur des candidats qui peuvent ne jamais être
     * acceptés. missionId volontairement null -- distingue une vraie
     * affectation committée (rang 1) d'une simple alternative (cf
     * Proposition.missionId, service-mkt, colonne nullable).
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
            PropositionEmiseEvent alternative = new PropositionEmiseEvent(
                    UUID.randomUUID(),
                    cycleMatchingIdsLigne[candidat.indice()],
                    demandeId,
                    capacitesReference.get(candidat.indice()),
                    null,
                    demande.axeId(),
                    rang,
                    rang == 2 ? "2e meilleur prix" : "3e meilleur prix",
                    BigDecimal.valueOf(candidat.cout()),
                    null,
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
