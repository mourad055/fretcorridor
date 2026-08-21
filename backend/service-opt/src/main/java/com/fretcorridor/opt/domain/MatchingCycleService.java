package com.fretcorridor.opt.domain;

import com.fretcorridor.dto.PointGeoDto;
import com.fretcorridor.opt.client.AxeActifDto;
import com.fretcorridor.opt.client.CandidatCoutDto;
import com.fretcorridor.opt.client.ServiceGeoClient;
import com.fretcorridor.util.HaversineUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Declencheur du cycle de matching "par fenetre", par axe actif (EF-MAT-01 -
 * jamais un matching immediat evenement par evenement). Tourne
 * periodiquement (spring.matching.cycle-interval-ms), regroupe tout ce qui
 * est en attente sur chaque axe ou matchingActif=true (EF-GEO-03), et lance
 * un seul L1 (Kuhn-Munkres) par axe sur le lot entier.
 *
 * Un axe sans demande ET sans capacite en attente est simplement ignore ce
 * tour - pas d'appel MAT/Kuhn-Munkres inutile (cf ServiceMatClient, evite de
 * gaspiller un cycle sur un lot vide).
 *
 * Un axe avec des demandes mais aucune capacite (ou l'inverse) reste lui
 * aussi en attente : AffectationL1Service exige que candidats soit non vide,
 * donc rien n'est tente tant que les deux cotes n'ont pas au moins une
 * entree - comportement voulu, pas une limitation a corriger.
 */
@Service
public class MatchingCycleService {

    private static final Logger log = LoggerFactory.getLogger(MatchingCycleService.class);

    private final ServiceGeoClient serviceGeoClient;
    private final CapaciteEnAttenteRepository capaciteEnAttenteRepository;
    private final DemandeEnAttenteRepository demandeEnAttenteRepository;
    private final AffectationL1Service affectationL1Service;

    // RG-105 (fenetre adaptative) : fenetre courante par axe, ajustee apres
    // chaque cycle traite selon le volume observe (taille du lot). Cle absente
    // = la base (parametre d'axe ou defaut global) sera utilisee au premier
    // tour de l'axe. En memoire uniquement : un redemarrage reinitialise
    // l'adaptation sur la base - acceptable, l'adaptation converge en quelques
    // cycles et aucune decision de matching n'en depend retroactivement.
    private final java.util.concurrent.ConcurrentHashMap<java.util.UUID, Double> fenetreAdaptiveParAxe =
            new java.util.concurrent.ConcurrentHashMap<>();

    // RG-105 : "La duree de la fenetre de traitement est un parametre d'axe,
    // ajuste en fonction du volume observe, avec une borne inferieure
    // garantissant qu'un lot contient en esperance plusieurs elements."
    // Defaut 0 = comportement historique (matching des qu'un lot est formable)
    // pour ne pas changer la demo sans configuration explicite ; les axes
    // denses doivent porter "fenetreTraitementSecondes" dans Axe.parametres.
    private final double fenetreDefautSecondes;
    private final double fenetreMinSecondes;
    private final double fenetreMaxSecondes;
    private final double facteurAdaptationFenetre;

    public MatchingCycleService(ServiceGeoClient serviceGeoClient,
                                 CapaciteEnAttenteRepository capaciteEnAttenteRepository,
                                 DemandeEnAttenteRepository demandeEnAttenteRepository,
                                 AffectationL1Service affectationL1Service,
                                 @org.springframework.beans.factory.annotation.Value(
                                         "${fretcorridor.opt.matching.fenetre-defaut-secondes:0}") double fenetreDefautSecondes,
                                 @org.springframework.beans.factory.annotation.Value(
                                         "${fretcorridor.opt.matching.fenetre-min-secondes:0}") double fenetreMinSecondes,
                                 @org.springframework.beans.factory.annotation.Value(
                                         "${fretcorridor.opt.matching.fenetre-max-secondes:3600}") double fenetreMaxSecondes,
                                 @org.springframework.beans.factory.annotation.Value(
                                         "${fretcorridor.opt.matching.facteur-adaptation-fenetre:2.0}") double facteurAdaptationFenetre) {
        this.serviceGeoClient = serviceGeoClient;
        this.capaciteEnAttenteRepository = capaciteEnAttenteRepository;
        this.demandeEnAttenteRepository = demandeEnAttenteRepository;
        this.affectationL1Service = affectationL1Service;
        this.fenetreDefautSecondes = fenetreDefautSecondes;
        this.fenetreMinSecondes = fenetreMinSecondes;
        this.fenetreMaxSecondes = fenetreMaxSecondes;
        this.facteurAdaptationFenetre = facteurAdaptationFenetre;
    }

    @Scheduled(fixedDelayString = "${spring.matching.cycle-interval-ms:15000}")
    public void executerCycle() {
        List<AxeActifDto> axesActifs = serviceGeoClient.axesActifsMatching();

        if (axesActifs.isEmpty()) {
            log.debug("Aucun axe actif pour le matching ce tour (ou service-geo injoignable).");
            return;
        }

        for (AxeActifDto axe : axesActifs) {
            traiterAxe(axe);
        }
    }

    private void traiterAxe(AxeActifDto axe) {
        List<CapaciteEnAttente> capacites = capaciteEnAttenteRepository.findByAxeIdAndTraiteeFalse(axe.id());
        List<DemandeEnAttente> demandes = demandeEnAttenteRepository.findByAxeIdAndTraiteeFalse(axe.id());

        if (capacites.isEmpty() || demandes.isEmpty()) {
            // Rien a apparier ce tour sur cet axe - reste en attente pour le prochain.
            return;
        }

        // RG-105 (fenetre adaptative par axe) : un element fraichement arrive
        // n'est eligible que once la fenetre de traitement de l'axe est ecoulee
        // depuis sa reception - c'est ce qui laisse les lots se former au lieu
        // de dispatcher en glouton desguise (le CDC vise explicitement "les lots
        // d'un element" comme le comportement a eviter sur les axes peu denses).
        java.time.Instant maintenant = java.time.Instant.now();
        double fenetreSecondes = fenetreEffective(axe);
        if (fenetreSecondes > 0) {
            int demandesAvant = demandes.size();
            int capacitesAvant = capacites.size();
            demandes = demandes.stream()
                    .filter(d -> ageFenetreAtteint(d.getDateReception(), maintenant, fenetreSecondes))
                    .toList();
            capacites = capacites.stream()
                    .filter(c -> ageFenetreAtteint(c.getDateReception(), maintenant, fenetreSecondes))
                    .toList();
            if (demandes.size() < demandesAvant || capacites.size() < capacitesAvant) {
                log.debug("Axe {} : {} demande(s)/{} capacite(s) encore dans la fenetre de traitement "
                                + "({} s) - exclus de ce cycle (RG-105).",
                        axe.nom(), demandesAvant - demandes.size(), capacitesAvant - capacites.size(),
                        fenetreSecondes);
            }
            if (capacites.isEmpty() || demandes.isEmpty()) {
                return; // tout le monde attend la fin de sa fenetre - prochain tour
            }
        }

        // Volet A Phase 4 (README_PHASE4_MOTEUR S2.2) : RISQUE_AXE, un des 7
        // termes du cout composite (CDC S8.5.3) prevu depuis le V0 mais jamais
        // branche jusqu'ici - la donnee existe cote GEO depuis le Sprint 15
        // (Axe.parametres.risqueSecuritaire) mais n'etait consommee par aucun
        // module. Valeur identique pour tous les candidats de ce cycle (le
        // risque est une propriete de l'axe, pas du candidat individuel).
        Double risqueAxeScore = extraireRisqueAxeScore(axe);

        List<CandidatCoutDto> candidatsCommuns = capacites.stream()
                .map(c -> new CandidatCoutDto(c.getCapaciteId(), c.getTransporteurId(),
                        c.getVehiculeId(), enrichirAvecRisqueAxe(c.getValeursCriteres(), risqueAxeScore),
                        c.getPosition(), c.getProfilCamion(), c.getTypeVehicule()))
                .toList();

        // EF-MAT-01/02/03 (Phase 1 MVP, priorite M) - RG-046 : rayonMatchingKm
        // lu depuis Axe.parametres (EF-GEO-02), jamais code en dur. Absence
        // de cle = pas de borne appliquee ce cycle (limitation documentee
        // dans AxeActifDto, pas une valeur par defaut inventee ici).
        //
        // CHOIX D'EQUIPE (le CDC ne precise pas explicitement "distance entre
        // quoi et quoi") : rayon autour de l'ORIGINE de la demande - RG-046
        // parle de "distance d'approche a vide", c-a-d la distance camion ->
        // point de collecte, pas la destination ni le trajet complet.
        // Coherent avec ZonageH3Service/hubs-proches (GEO), qui filtre deja
        // par rayon autour d'un point de collecte unique.
        Double rayonMatchingKm = extraireRayonMatchingKm(axe);

        List<DemandeAvecCandidats> lot = demandes.stream()
                .map(d -> new DemandeAvecCandidats(d.getDemandeId(), d.getOrigine(), d.getDestination(),
                        d.getAxeId(), d.getPoidsTaxableKg(), filtrerCandidatsParRayon(d, candidatsCommuns, rayonMatchingKm)))
                .toList();

        // Une demande dont TOUS les candidats ont ete elimines par le rayon
        // n'a plus de sens a envoyer a Kuhn-Munkres (AffectationL1Service
        // exige des candidats non vides par demande) - filtree du lot plutot
        // que de faire planter le L1 sur une liste vide.
        List<DemandeAvecCandidats> lotNonVide = lot.stream()
                .filter(d -> !d.candidats().isEmpty())
                .toList();

        if (lotNonVide.isEmpty()) {
            log.debug("Aucun candidat dans le rayon d'appariement sur l'axe {} ce tour - "
                    + "demandes/capacites laissees en attente pour le prochain cycle.", axe.nom());
            return;
        }

        log.info("Cycle de matching declenche - axe={}, {} demande(s), {} capacite(s) en attente "
                        + "(rayon={} km)",
                axe.nom(), demandes.size(), capacites.size(), rayonMatchingKm);

        AffectationLotResultat resultat = affectationL1Service.calculerAffectationOptimale(lotNonVide);

        if (resultat.modeDegrade()) {
            log.warn("Cycle en mode degrade sur l'axe {} - service-mat injoignable, "
                    + "capacites/demandes laissees en attente pour le prochain tour.", axe.nom());
            return; // ne marque rien comme traite : on retentera au cycle suivant (ENF-DIS-04)
        }

        // BUG CORRIGE (2026-08-18) : le code precedent marquait TOUT le lot
        // (demandes ET capacites) comme traite des que le resultat n'etait
        // pas en mode degrade, sans verifier lesquelles avaient reellement
        // ete affectees. Kuhn-Munkres etant un appariement optimal, un lot
        // avec plus de demandes que de capacites (cas courant) laisse
        // certaines demandes avec capaciteId()==null (cf AffectationResultat
        // javadoc, "plus de demandes que de capacites disponibles dans le
        // lot") - ces demandes etaient neanmoins marquees traitee=true et
        // disparaissaient silencieusement du systeme, en violation de
        // EF-MAT-01 (traitement par cycles a fenetre, jamais de perte) et
        // EF-MAT-11/12 (traçabilite/reconstitution de chaque decision).
        Set<java.util.UUID> demandesAffectees = resultat.affectations().stream()
                .filter(a -> a.capaciteId() != null)
                .map(AffectationResultat::demandeId)
                .collect(Collectors.toSet());
        Set<java.util.UUID> capacitesAffectees = resultat.affectations().stream()
                .filter(a -> a.capaciteId() != null)
                .map(AffectationResultat::capaciteId)
                .collect(Collectors.toSet());

        List<DemandeEnAttente> demandesTraitees = demandes.stream()
                .filter(d -> demandesAffectees.contains(d.getDemandeId()))
                .toList();
        demandesTraitees.forEach(DemandeEnAttente::marquerTraitee);
        demandeEnAttenteRepository.saveAll(demandesTraitees);

        List<CapaciteEnAttente> capacitesTraitees = capacites.stream()
                .filter(c -> capacitesAffectees.contains(c.getCapaciteId()))
                .toList();
        capacitesTraitees.forEach(CapaciteEnAttente::marquerTraitee);
        capaciteEnAttenteRepository.saveAll(capacitesTraitees);

        int demandesNonAffecteesCeCycle = demandes.size() - demandesTraitees.size();
        if (demandesNonAffecteesCeCycle > 0) {
            log.info("Cycle sur l'axe {} : {} demande(s) non affectee(s) ce tour (plus de demandes "
                            + "que de capacites disponibles), laissee(s) en attente pour le prochain cycle.",
                    axe.nom(), demandesNonAffecteesCeCycle);
        }

        // RG-105 : ajuste la fenetre de l'axe selon le volume observe - un lot
        // d'un element est precisement le "dispatch glouton deguise" que le
        // CDC ordonne d'eviter, on attend donc plus la fois suivante ; un lot
        // riche montre que la fenetre peut se raccourcir sans degrade le
        // remplissage. Trace a chaque ajustement (EF-MAT-11).
        double fenetreAvant = fenetreSecondes;
        double fenetreApres = ajusterFenetre(fenetreSecondes, lotNonVide.size(),
                facteurAdaptationFenetre, fenetreMinSecondes, fenetreMaxSecondes);
        if (fenetreApres != fenetreAvant) {
            fenetreAdaptiveParAxe.put(axe.id(), fenetreApres);
            log.info("RG-105 axe {} : fenetre de traitement ajustee {} s -> {} s "
                            + "(lot observe de {} element(s)).",
                    axe.nom(), fenetreAvant, fenetreApres, lotNonVide.size());
        }
    }

    /**
     * Fenetre de traitement effective d'un axe (RG-105), en secondes :
     * parametre d'axe "fenetreTraitementSecondes" si present dans
     * Axe.parametres (EF-GEO-02, meme pattern que rayonAppariementKm),
     * sinon defaut global configure ; la valeur adaptative courante (si
     * deja ajustee sur cet axe) est bornee aux bornes configurees.
     */
    private double fenetreEffective(AxeActifDto axe) {
        Double parametreAxe = extraireFenetreParametreAxe(axe.parametres());
        double base = parametreAxe != null ? parametreAxe : fenetreDefautSecondes;
        return bornerFenetre(fenetreAdaptiveParAxe.getOrDefault(axe.id(), base));
    }

    /**
     * Lit "fenetreTraitementSecondes" dans Axe.parametres. Absente ou de type
     * inattendu = null, comme rayonAppariementKm - pas de defaut invente au
     * niveau de l'axe, le defaut global configure prend le relais.
     */
    static Double extraireFenetreParametreAxe(Map<String, Object> parametres) {
        if (parametres == null) {
            return null;
        }
        Object valeur = parametres.get("fenetreTraitementSecondes");
        if (valeur instanceof Number nombre) {
            return nombre.doubleValue();
        }
        return null;
    }

    /**
     * Un element est eligible au matching once son age dans la file atteint
     * la fenetre de traitement. dateReception null ne doit theoriquement pas
     * arriver (colonne NOT NULL) - defensivement, on n'exclut jamais un
     * element sur une donnee manquante (on ne veut pas de file qui se vide
     * par bug de donnee).
     */
    static boolean ageFenetreAtteint(java.time.Instant dateReception,
                                      java.time.Instant maintenant, double fenetreSecondes) {
        if (dateReception == null || fenetreSecondes <= 0) {
            return true;
        }
        return java.time.Duration.between(dateReception, maintenant).getSeconds() >= fenetreSecondes;
    }

    /**
     * Ajustement multiplicatif de la fenetre selon le volume observe (RG-105)
     * : lot d'un seul element -> fenetre x facteur (les arrivees etaient trop
     * isolees, il faut accumuler plus longtemps) ; lot de plusieurs elements
     * -> fenetre / facteur (le remplissage est au rendez-vous, on reagit plus
     * vite). Toujours borne [min, max].
     */
    static double ajusterFenetre(double fenetreActuelle, int tailleLotObservee,
                                  double facteur, double min, double max) {
        double ajustee = tailleLotObservee <= 1 ? fenetreActuelle * facteur : fenetreActuelle / facteur;
        return bornerFenetreStatic(ajustee, min, max);
    }

    static double bornerFenetreStatic(double valeur, double min, double max) {
        return Math.max(min, Math.min(max, valeur));
    }

    private double bornerFenetre(double valeur) {
        return bornerFenetreStatic(valeur, fenetreMinSecondes, fenetreMaxSecondes);
    }

    /**
     * Lit "rayonAppariementKm" dans Axe.parametres (EF-GEO-02, JSONB jamais code
     * en dur). Retourne null si la cle est absente ou de type inattendu -
     * dans ce cas aucun filtre n'est applique, comportement volontairement
     * permissif plutot qu'un defaut invente.
     */
    private Double extraireRayonMatchingKm(AxeActifDto axe) {
        Map<String, Object> parametres = axe.parametres();
        if (parametres == null) {
            return null;
        }
        Object valeur = parametres.get("rayonAppariementKm");
        if (valeur instanceof Number nombre) {
            return nombre.doubleValue();
        }
        if (valeur != null) {
            log.warn("rayonAppariementKm present sur l'axe {} mais de type inattendu ({}) - "
                    + "filtre ignore ce tour.", axe.nom(), valeur.getClass().getSimpleName());
        }
        return null;
    }

    /**
     * Lit Axe.parametres.risqueSecuritaire.niveauRisque (cote GEO, AxeController,
     * Sprint 15) et le convertit en score [0,1] pour le critere RISQUE_AXE du
     * cout composite (CDC S8.5.3).
     *
     * HYPOTHESE D'EQUIPE (a valider, pas une valeur du CDC - meme statut que
     * l'hypothese RG-116 deja assumee cote TarificationL4Service) :
     *   NORMAL ou cle absente -> 0.0 (aucune penalite, meme defaut permissif
     *                                  que rayonAppariementKm absent)
     *   SURVEILLE            -> 0.5 (score intermediaire, aucune justification
     *                                 chiffree du CDC - a confirmer en equipe)
     *   GELE                  -> n'arrive jamais ici : un axe GELE est deja
     *                             exclu de axesActifsMatching() cote GEO
     *                             (AxeController.estOperationnelPourMatching),
     *                             donc ce cycle ne le traite jamais.
     */
    private Double extraireRisqueAxeScore(AxeActifDto axe) {
        Map<String, Object> parametres = axe.parametres();
        if (parametres == null) {
            return 0.0;
        }
        Object risque = parametres.get("risqueSecuritaire");
        if (!(risque instanceof Map<?, ?> risqueMap)) {
            return 0.0;
        }
        Object niveau = risqueMap.get("niveauRisque");
        if ("SURVEILLE".equals(niveau)) {
            return 0.5;
        }
        return 0.0;
    }

    /**
     * Ajoute RISQUE_AXE aux criteres d'un candidat, sans modifier les criteres
     * deja calcules ailleurs (cf CapaciteEnAttente.getValeursCriteres()).
     * risqueAxeScore==null n'arrive jamais en pratique (extraireRisqueAxeScore
     * retourne toujours 0.0 au minimum) mais garde-fou defensif quand meme,
     * meme principe que les autres degradations gracieuses de ce fichier.
     */
    private Map<String, Double> enrichirAvecRisqueAxe(Map<String, Double> valeursCriteres, Double risqueAxeScore) {
        if (risqueAxeScore == null) {
            return valeursCriteres;
        }
        Map<String, Double> enrichi = new java.util.LinkedHashMap<>(valeursCriteres);
        enrichi.put("RISQUE_AXE", risqueAxeScore);
        return enrichi;
    }

    /**
     * Filtre les candidats dont la position est a plus de rayonKm de
     * l'origine de la demande (Haversine, RG-046 : "distance d'approche a
     * vide"). Aucun filtre applique si rayonKm est null (cle absente) ou si
     * l'origine de la demande / la position d'un candidat est inconnue -
     * degrade vers "pas de borne" plutot que d'exclure silencieusement un
     * candidat sur une donnee manquante (ENF-DIS-04).
     */
    private List<CandidatCoutDto> filtrerCandidatsParRayon(DemandeEnAttente demande,
                                                            List<CandidatCoutDto> candidats,
                                                            Double rayonKm) {
        PointGeoDto origine = demande.getOrigine();
        if (rayonKm == null || origine == null) {
            return candidats;
        }

        return candidats.stream()
                .filter(c -> {
                    PointGeoDto position = c.positionCapacite();
                    if (position == null) {
                        return true; // position inconnue : ne pas exclure silencieusement
                    }
                    return HaversineUtils.distance(origine, position) <= rayonKm;
                })
                .toList();
    }
}
