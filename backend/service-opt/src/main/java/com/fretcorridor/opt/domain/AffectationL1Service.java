package com.fretcorridor.opt.domain;

import com.fretcorridor.opt.client.CandidatCoutDto;
import com.fretcorridor.opt.client.CoutLotRequestDto;
import com.fretcorridor.opt.client.CoutLotResponseDto;
import com.fretcorridor.opt.client.CoutResponseDto;
import com.fretcorridor.opt.client.ItineraireRequestDto;
import com.fretcorridor.opt.client.ItineraireResponseDto;
import com.fretcorridor.opt.client.ServiceMatClient;
import com.fretcorridor.opt.client.ValhallaClient;
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
import java.util.List;
import java.util.UUID;

/**
 * Orchestration du L1 (Sprint 5, revu diffusion-course - plan de
 * reorientation post-demo).
 *
 * CHANGEMENT DE MODELE (remplace le Kuhn-Munkres "1 gagnant par demande" du
 * CDC EF-MKT-06/07) : pour chaque demande, une Affectation PROPOSEE est
 * creee ET une PropositionEmise est publiee pour CHAQUE candidat dont le
 * cout compose n'est pas une sentinelle (donc compatible - le filtrage
 * physique/geospatial est deja fait en amont, cf MatchingCycleService.
 * filtrerCandidatsL0/filtrerCapacitePhysique). Aucun candidat n'est choisi
 * ici : la selection reelle se fait cote chauffeur (premier a accepter,
 * cf AffectationConfirmationService/DemandeAccepteeListener), jamais a
 * cette etape.
 *
 * CE QUI N'EST PLUS FAIT ICI (deplace vers AffectationConfirmationService,
 * qui ne s'execute qu'a l'acceptation reelle d'un chauffeur - jamais avant) :
 *   - reservation de capacite (ServiceCapClient.reserver)
 *   - publication AffectationConfirmeeEvent (-> service-exe)
 *   - RepartitionConventionnelleAppliquee (-> service-pay)
 * Publier ces trois choses ici, comme avant, reviendrait a confirmer une
 * mission avant qu'aucun chauffeur n'ait rien accepte - contraire au modele
 * diffusion-course ET au modele CDC (RG-050 : "a l'acceptation, la capacite
 * est reservee atomiquement").
 *
 * LIMITATION CONNUE, documentee plutot que corrigee ce soir : rien
 * n'empeche aujourd'hui qu'une meme capacite recoive des propositions
 * PROPOSEE concurrentes issues de DEUX demandes differentes (le filtre
 * physique verifie le reliquat declare, pas les reservations "en cours de
 * course" d'autres demandes). Si un chauffeur accepte les deux, la seconde
 * confirmation reelle echouera au moment de ServiceCapClient.reserver
 * (refus service-cap, capacite insuffisante) plutot qu'a la diffusion - a
 * traiter si observe en pratique, pas invente ici.
 */
@Service
public class AffectationL1Service {

    private static final Logger log = LoggerFactory.getLogger(AffectationL1Service.class);

    private final ServiceMatClient serviceMatClient;
    private final ValhallaClient valhallaClient;
    private final TarificationL4Service tarificationL4Service;
    private final AffectationRepository affectationRepository;
    private final OptEventPublisher eventPublisher;
    private final CompatibiliteMarchandisesService compatibiliteMarchandisesService;

    public AffectationL1Service(ServiceMatClient serviceMatClient, ValhallaClient valhallaClient,
                                 TarificationL4Service tarificationL4Service,
                                 AffectationRepository affectationRepository,
                                 OptEventPublisher eventPublisher,
                                 CompatibiliteMarchandisesService compatibiliteMarchandisesService) {
        this.serviceMatClient = serviceMatClient;
        this.valhallaClient = valhallaClient;
        this.tarificationL4Service = tarificationL4Service;
        this.affectationRepository = affectationRepository;
        this.eventPublisher = eventPublisher;
        this.compatibiliteMarchandisesService = compatibiliteMarchandisesService;
    }

    public AffectationLotResultat calculerAffectationOptimale(List<DemandeAvecCandidats> demandes) {
        return calculerAffectationOptimale(demandes, null, null);
    }

    public AffectationLotResultat calculerAffectationOptimale(List<DemandeAvecCandidats> demandes,
                                                                String origineNom, String destinationNom) {
        if (demandes == null || demandes.isEmpty()) {
            return new AffectationLotResultat(false, List.of());
        }

        List<AffectationResultat> resultatsFinaux = new ArrayList<>();

        for (DemandeAvecCandidats demande : demandes) {
            UUID demandeId = demande.demandeId();

            if (demande.origineDemande() == null || demande.destinationDemande() == null) {
                log.warn("Demande {} sans coordonnees origine/destination - aucune diffusion ce cycle.",
                        demandeId);
                resultatsFinaux.add(new AffectationResultat(demandeId, null, null, null, null, null, null));
                continue;
            }

            // Point 5 (matrice compatibilite marchandises) : REGLE DE FILTRAGE
            // DURE APPLIQUEE AVANT LE CALCUL DE COUT MAT. Une capacite qui
            // transporte deja une demande au profil marchandise incompatible
            // (anti-groupage matieres dangereuses ou paire de la matrice) est
            // exclue du lot L1 - jamais une penalite de cout, une vraie
            // exclusion. Candidate sans detail lot (permissif) ou sans aucune
            // demande confirmee sur la capacite : non exclue.
            List<CandidatCoutDto> candidatsCompatibles = new ArrayList<>(demande.candidats());
            candidatsCompatibles.removeIf(candidat ->
                    !compatibiliteMarchandisesService.compatibleAvecDemandesDeLaCapacite(
                            demandeId, candidat.capaciteId()));
            if (candidatsCompatibles.size() != demande.candidats().size()) {
                log.info("Filtre dur compatibilite marchandises : {} candidat(s) exclu(s) pour la demande {} "
                                + "(sur {} candidats L0)",
                        demande.candidats().size() - candidatsCompatibles.size(),
                        demandeId, demande.candidats().size());
            }
            if (candidatsCompatibles.isEmpty()) {
                log.debug("Demande {} : aucun candidat compatible marchandises ce cycle.", demandeId);
                resultatsFinaux.add(new AffectationResultat(demandeId, null, null, null, null, null, null));
                continue;
            }

            CoutLotResponseDto reponse = serviceMatClient.calculerCoutsLot(
                    new CoutLotRequestDto(demandeId, demande.axeId(), candidatsCompatibles));

            if (reponse == null) {
                log.warn("service-mat injoignable pour la demande {} - lot L1 en mode degrade, "
                        + "aucune diffusion produite.", demandeId);
                return new AffectationLotResultat(true, List.of());
            }

            List<CoutResponseDto> resultatsCout = reponse.resultats();
            boolean auMoinsUneDiffusion = false;

            for (int j = 0; j < candidatsCompatibles.size(); j++) {
                double cout = resultatsCout.get(j).coutTotal().doubleValue();
                if (cout >= com.fretcorridor.opt.algorithm.KuhnMunkresSolver.COUT_SENTINELLE) {
                    // Sentinelle = incompatible (deja hors rayon/detour/essieu selon
                    // le critere qui l'a produite cote service-mat) - jamais diffuse.
                    continue;
                }

                CandidatCoutDto candidat = candidatsCompatibles.get(j);
                ItineraireResponseDto itineraire = calculerItineraireSiPossible(demande, candidat);
                Double distanceMetres = itineraire != null ? itineraire.distanceMetres() : null;
                TarificationResultat tarification = tarificationL4Service.calculer(
                        demande.axeId(), candidat.typeVehicule(), demande.poidsTaxableKg(),
                        distanceMetres, BigDecimal.ZERO);

                Affectation affectation = new Affectation(
                        demandeId, candidat.capaciteId(), resultatsCout.get(j).cycleMatchingId(), demande.axeId(),
                        demande.poidsTaxableKg(),
                        demande.origineDemande().latitude(), demande.origineDemande().longitude(),
                        demande.destinationDemande().latitude(), demande.destinationDemande().longitude(),
                        itineraire != null ? itineraire.distanceMetres() : null,
                        itineraire != null ? itineraire.dureeSecondes() : null,
                        itineraire != null ? itineraire.intervalleConfianceSecondes() : null,
                        itineraire != null ? itineraire.geometrieEncodee() : null,
                        BigDecimal.valueOf(cout),
                        tarification.baremeId(), tarification.baremeVersion(), tarification.regime(),
                        tarification.coutBase(), tarification.coutVariablePoidsTaxable(),
                        tarification.coutServices(), tarification.facteurTensionApplique(),
                        tarification.prixTransportAvantPlancher(), tarification.plancherApplique(),
                        tarification.prixTransport(), tarification.commissionPlateforme(),
                        tarification.montantVerseTransporteur(), tarification.modeDegrade(),
                        candidat.vehiculeId(), demande.typeEmballageNom(), demande.quantite(),
                        demande.destinataireNom(), demande.destinataireTelephone(),
                        demande.modeCollecte(), demande.typeDisponibilite(),
                        demande.poidsTotalKg(), demande.grandeValeur());

                UUID affectationId = affectationRepository.save(affectation).getId();
                auMoinsUneDiffusion = true;

                resultatsFinaux.add(new AffectationResultat(
                        demandeId, candidat.capaciteId(), affectationId,
                        BigDecimal.valueOf(cout), resultatsCout.get(j).cycleMatchingId(),
                        itineraire, tarification));

                if (tarification.modeDegrade()) {
                    // Pas de prix fiable pour ce candidat precis (cf ENF-DIS-04) -
                    // l'Affectation existe (tracabilite), mais rien a diffuser au
                    // chauffeur tant qu'aucun prix n'est disponible.
                    continue;
                }

                PropositionEmiseEvent proposition = new PropositionEmiseEvent(
                        UUID.randomUUID(),
                        resultatsCout.get(j).cycleMatchingId(),
                        demandeId,
                        candidat.capaciteId(),
                        affectationId,
                        demande.axeId(),
                        j + 1, // informationnel uniquement - diffusion-course n'ordonne plus,
                               // conserve pour compat avec le contrat existant (pas de
                               // "classement" fonctionnel dessus)
                        "Diffuse a tout chauffeur compatible - premier arrive gagne",
                        tarification.prixTransport(),
                        tarification.commissionPlateforme(),
                        "XAF",
                        itineraire != null ? itineraire.distanceMetres() : 0,
                        itineraire != null ? (long) itineraire.dureeSecondes() : null,
                        origineNom,
                        destinationNom,
                        Instant.now());
                eventPublisher.publierPropositionEmise(proposition);
            }

            if (!auMoinsUneDiffusion) {
                log.debug("Aucun candidat compatible pour la demande {} ce cycle - reste en attente.",
                        demandeId);
                resultatsFinaux.add(new AffectationResultat(demandeId, null, null, null, null, null, null));
            }
        }

        return new AffectationLotResultat(false, resultatsFinaux);
    }

    private ItineraireResponseDto calculerItineraireSiPossible(DemandeAvecCandidats demande,
                                                                 CandidatCoutDto candidat) {
        if (candidat.positionCapacite() == null
                || demande.origineDemande() == null
                || demande.destinationDemande() == null) {
            log.warn("Coordonnees incompletes pour la demande {} / capacite {} - "
                            + "itineraire Valhalla non calcule (mode degrade sur ce candidat uniquement).",
                    demande.demandeId(), candidat.capaciteId());
            return null;
        }

        ItineraireRequestDto requete = new ItineraireRequestDto(
                List.of(candidat.positionCapacite(), demande.origineDemande(), demande.destinationDemande()),
                candidat.profilCamion());

        return valhallaClient.calculerItineraire(requete);
    }
}
