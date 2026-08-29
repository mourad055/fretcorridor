package com.fretcorridor.opt.domain;

import com.fretcorridor.dto.PointGeoDto;
import com.fretcorridor.opt.client.AxeActifDto;
import com.fretcorridor.opt.client.CandidatCoutDto;
import com.fretcorridor.opt.client.PositionActuelleDto;
import com.fretcorridor.opt.client.ServiceGeoClient;
import com.fretcorridor.opt.client.ServiceTrkClient;
import com.fretcorridor.opt.config.ServiceTrkClientProperties;
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
    private final ServiceTrkClient serviceTrkClient;
    private final CapaciteEnAttenteRepository capaciteEnAttenteRepository;
    private final DemandeEnAttenteRepository demandeEnAttenteRepository;
    private final AffectationL1Service affectationL1Service;
    private final CalculateurCoutSeptTermes calculateurCoutSeptTermes;
    private final InstrumentationPerfService instrumentationPerfService;

    // Plan de reorientation post-demo, partie Chauffeur point 1 : "prendre en
    // compte la position gps du chauffeur" (temps reel, pas seulement la
    // position declaree a CapaciteDeclareeEvent). Seuil de fraicheur -
    // HYPOTHESE D'EQUIPE (cf ServiceTrkClientProperties javadoc), pas une
    // valeur imposee par le plan lui-meme.
    private final long positionMaxAgeSecondes;

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

    // Filtrage L0 par cellules H3 (CDC S8.5.4/S8.10) : nombre d'anneaux de
    // voisines autour de la cellule de l'origine de la demande. 1 = la cellule
    // + ses 6 voisines directes ; plafond impose cote GEO a 3.
    private final int l0KRing;

    public MatchingCycleService(ServiceGeoClient serviceGeoClient,
                                 ServiceTrkClient serviceTrkClient,
                                 ServiceTrkClientProperties serviceTrkClientProperties,
                                 CapaciteEnAttenteRepository capaciteEnAttenteRepository,
                                 DemandeEnAttenteRepository demandeEnAttenteRepository,
                                 AffectationL1Service affectationL1Service,
                                 CalculateurCoutSeptTermes calculateurCoutSeptTermes,
                                 InstrumentationPerfService instrumentationPerfService,
                                 @org.springframework.beans.factory.annotation.Value(
                                         "${fretcorridor.opt.matching.fenetre-defaut-secondes:0}") double fenetreDefautSecondes,
                                 @org.springframework.beans.factory.annotation.Value(
                                         "${fretcorridor.opt.matching.fenetre-min-secondes:0}") double fenetreMinSecondes,
                                 @org.springframework.beans.factory.annotation.Value(
                                         "${fretcorridor.opt.matching.fenetre-max-secondes:3600}") double fenetreMaxSecondes,
                                 @org.springframework.beans.factory.annotation.Value(
                                         "${fretcorridor.opt.matching.facteur-adaptation-fenetre:2.0}") double facteurAdaptationFenetre,
                                 @org.springframework.beans.factory.annotation.Value(
                                         "${fretcorridor.opt.matching.l0-k-ring:1}") int l0KRing) {
        this.serviceGeoClient = serviceGeoClient;
        this.serviceTrkClient = serviceTrkClient;
        this.positionMaxAgeSecondes = serviceTrkClientProperties.getPositionMaxAgeSecondes();
        this.capaciteEnAttenteRepository = capaciteEnAttenteRepository;
        this.demandeEnAttenteRepository = demandeEnAttenteRepository;
        this.affectationL1Service = affectationL1Service;
        this.calculateurCoutSeptTermes = calculateurCoutSeptTermes;
        this.instrumentationPerfService = instrumentationPerfService;
        this.fenetreDefautSecondes = fenetreDefautSecondes;
        this.fenetreMinSecondes = fenetreMinSecondes;
        this.fenetreMaxSecondes = fenetreMaxSecondes;
        this.facteurAdaptationFenetre = facteurAdaptationFenetre;
        this.l0KRing = Math.max(0, Math.min(3, l0KRing));
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

        long debutCycleNano = System.nanoTime();

        // RISQUE_AXE (6e terme du cout composite, CDC S8.5.3) : la donnee
        // existe cote GEO depuis le Sprint 15 (Axe.parametres.risqueSecuritaire).
        // Valeur identique pour tous les candidats de ce cycle (le risque est
        // une propriete de l'axe, pas du candidat individuel).
        Double risqueAxeScore = extraireRisqueAxeScore(axe);

        java.time.Instant instantCalcul = java.time.Instant.now();

        // VALEUR_RETOUR (7e terme, prospectif) : densite de fret en attente
        // autour de la destination de chaque demande - calculee une fois par
        // cycle, partagee entre toutes les paires de cette demande.
        Map<java.util.UUID, Long> fretAuRetourParDemande =
                compterAutresDemandesProchesDestination(demandes);

        // ===== L0 - Filtrage geospatial des candidats (CDC S8.5.4, budget < 50ms) =====
        // Priorite a l'indexation H3 (endpoints zonage/index + zonage/k-ring de
        // GEO) : cellule de l'origine de la demande + k anneaux de voisines ;
        // repli sur le rayon Haversine historique (RG-046) si GEO est injoignable
        // ou non configure (ENF-DIS-04 : on degrade, on ne plante jamais).
        // Les 7 criteres CDC S8.5.3 sont ensuite calcules PAR PAIRE sur les
        // candidats survivants (convention DemandeAvecCandidats : valeurs
        // "deja normalisees pour cette paire demande/candidat").
        long debutL0Nano = System.nanoTime();
        Double rayonMatchingKm = extraireRayonMatchingKm(axe);
        Map<PointGeoDto, String> cacheCellulesH3 = new java.util.HashMap<>();
        boolean[] l0H3Utilise = { false };
        // Copie finale pour la lambda : la variable capacites est reassignee
        // plus haut par le filtrage RG-105, donc pas "effectively final".
        List<CapaciteEnAttente> capacitesDuCycle = capacites;

        // Plan de reorientation post-demo, partie Chauffeur point 1 : position
        // GPS temps reel du chauffeur dans le matching. Resolue UNE FOIS par
        // cycle (pas par paire demande/candidat) pour rester dans le budget
        // de latence - un vehicule interroge plusieurs fois recevrait sinon
        // N appels HTTP identiques a service-trk. Cle absente ou position
        // trop vieille (positionMaxAgeSecondes) = repli sur la position
        // declaree, jamais un candidat exclu pour une position temps reel
        // manquante ou perimee (ENF-DIS-04).
        Map<java.util.UUID, PointGeoDto> positionsTempsReelParVehicule =
                resoudrePositionsTempsReel(capacitesDuCycle);

        List<DemandeAvecCandidats> lot = demandes.stream()
                .map(d -> {
                    List<CapaciteEnAttente> capacitesFiltrees =
                            filtrerCandidatsL0(d, capacitesDuCycle, rayonMatchingKm, cacheCellulesH3, l0H3Utilise,
                                    positionsTempsReelParVehicule);
                    // Diffusion-course (partie Chauffeur point 2) : exclusion
                    // des chauffeurs ayant refuse CETTE demande. On ecarte ici
                    // les capacites dont le transporteur est dans la liste
                    // d'exclusion (DemandeEnAttente.transporteursExclus,
                    // alimentee par DemandeRefuseeParChauffeurListener) - on ne
                    // re-diffuse jamais sur la meme demande a un chauffeur qui
                    // vient de refuser.
                    Set<java.util.UUID> exclus = d.getTransporteursExclus();
                    if (exclus != null && !exclus.isEmpty()) {
                        capacitesFiltrees = capacitesFiltrees.stream()
                                .filter(c -> !exclus.contains(c.getTransporteurId()))
                                .toList();
                    }
                    long fretAuRetour = fretAuRetourParDemande.getOrDefault(d.getDemandeId(), 0L);
                    List<CandidatCoutDto> candidatsAvecCriteres = capacitesFiltrees.stream()
                            .map(c -> construireCandidatAvecCriteresCDC(
                                    d, c, instantCalcul, fretAuRetour, risqueAxeScore, positionsTempsReelParVehicule))
                            .toList();
                    // Infos destinataire (audit de suivi Mobile, fusion dev/backend-stevetelecom) :
                    // propagees comme les autres champs marchandise, aucun impact sur le pipeline
                    // L0/7-termes ci-dessus - juste des donnees supplementaires portees jusqu'a
                    // AffectationConfirmeeEvent.
                    return new DemandeAvecCandidats(d.getDemandeId(), d.getOrigine(), d.getDestination(),
                            d.getAxeId(), d.getPoidsTaxableKg(), candidatsAvecCriteres,
                            d.getTypeEmballageNom(), d.getQuantite(),
                            d.getDestinataireNom(), d.getDestinataireTelephone(),
                            d.getModeCollecte(), d.getTypeDisponibilite(),
                            d.getPoidsTotalKg(), d.getGrandeValeur());
                })
                .toList();
        instrumentationPerfService.recordL0FiltrageMs(
                (System.nanoTime() - debutL0Nano) / 1_000_000.0);

        // Une demande dont TOUS les candidats ont ete elimines par le rayon
        // n'a plus de sens a envoyer a Kuhn-Munkres (AffectationL1Service
        // exige des candidats non vides par demande) - filtree du lot plutot
        // que de faire planter le L1 sur une liste vide.
        //
        // BUG CORRIGE (audit de suivi) : une demande sans coordonnees
        // (origine/destination null, ex. donnee de test incomplete) etait
        // laissee dans le lot envoye a Kuhn-Munkres. Les couts MAT etant
        // aujourd'hui des placeholders identiques pour tout candidat, rien
        // ne distinguait cette demande "morte" d'une demande valide : le
        // solveur pouvait tres bien lui affecter une capacite reelle de
        // maniere optimale (cout egal), que AffectationL1Service rejetait
        // ensuite (coordonnees manquantes pour persister l'Affectation) --
        // gaspillant cette capacite pour tout le cycle au lieu de la laisser
        // a une demande valide du meme lot. Filtree ici, en amont du
        // solveur, plutot que rejetee apres coup.
        List<DemandeAvecCandidats> lotNonVide = lot.stream()
                .filter(d -> !d.candidats().isEmpty())
                .filter(d -> d.origineDemande() != null && d.destinationDemande() != null)
                .toList();

        if (lotNonVide.isEmpty()) {
            log.debug("Aucun candidat dans le rayon d'appariement sur l'axe {} ce tour - "
                    + "demandes/capacites laissees en attente pour le prochain cycle.", axe.nom());
            return;
        }

        String modeL0 = l0H3Utilise[0]
                ? "H3(k=" + l0KRing + ")"
                : (rayonMatchingKm != null ? "rayon=" + rayonMatchingKm + "km" : "aucun filtre");
        log.info("Cycle de matching declenche - axe={}, {} demande(s), {} capacite(s) en attente, L0={}",
                axe.nom(), demandes.size(), capacites.size(), modeL0);

        long debutL1Nano = System.nanoTime();
        AffectationLotResultat resultat = affectationL1Service.calculerAffectationOptimale(
                lotNonVide, axe.hubOrigineVille(), axe.hubDestinationVille());
        instrumentationPerfService.recordL1AffectationMs(
                (System.nanoTime() - debutL1Nano) / 1_000_000.0);

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
        // Diffusion-course (plan de reorientation) : une demande est "traitee"
        // des que la diffusion a eu lieu (des PropositionEmise sont parties),
        // meme si aucun chauffeur n'a encore accepte - sinon elle reviendrait
        // dans CE cycle a chaque tour tant que personne n'a accepte.
        //
        // Une capacite, en revanche, N'EST JAMAIS marquee traitee ici : elle
        // doit rester visible/proposable a d'autres demandes du meme cycle ou
        // des cycles suivants tant qu'aucune Affectation la concernant n'est
        // reellement CONFIRMEE (AffectationConfirmationService, sur
        // acceptation reelle d'un chauffeur) - marquer la capacite ici la
        // rendrait invisible aux autres demandes pendant qu'elle n'a encore
        // rien accepte, contraire a l'esprit "diffusion a tous les chauffeurs
        // compatibles" du plan de reorientation.
        Set<java.util.UUID> demandesAffectees = resultat.affectations().stream()
                .filter(a -> a.capaciteId() != null)
                .map(AffectationResultat::demandeId)
                .collect(Collectors.toSet());

        List<DemandeEnAttente> demandesTraitees = demandes.stream()
                .filter(d -> demandesAffectees.contains(d.getDemandeId()))
                .toList();
        demandesTraitees.forEach(DemandeEnAttente::marquerTraitee);
        demandeEnAttenteRepository.saveAll(demandesTraitees);

        int demandesNonAffecteesCeCycle = demandes.size() - demandesTraitees.size();
        if (demandesNonAffecteesCeCycle > 0) {
            log.info("Cycle sur l'axe {} : {} demande(s) non affectee(s) ce tour (plus de demandes "
                            + "que de capacites disponibles), laissee(s) en attente pour le prochain cycle.",
                    axe.nom(), demandesNonAffecteesCeCycle);
        }

        // EF-PERF (CDC S8.10) : latence bout-en-bout des demandes affectees
        // (date_reception -> fin du cycle d'affectation).
        java.time.Instant finCycle = java.time.Instant.now();
        for (DemandeEnAttente d : demandesTraitees) {
            if (d.getDateReception() != null) {
                instrumentationPerfService.recordLatenceAffectationSecondes(
                        java.time.Duration.between(d.getDateReception(), finCycle).toMillis() / 1000.0);
            }
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

        // EF-PERF (CDC S8.10) : duree totale du cycle de cet axe (L0+L1+persistance).
        instrumentationPerfService.recordCycleAxeMs((System.nanoTime() - debutCycleNano) / 1_000_000.0);
    }

    /**
     * Interroge service-trk pour chaque vehicule distinct du lot de
     * capacites de ce cycle, une seule fois par vehicule (pas par paire).
     * Cle absente dans la map retournee = pas de position temps reel connue
     * ou trop vieille -> l'appelant doit retomber sur la position declaree
     * (cf resoudrePositionEffective).
     */
    private Map<java.util.UUID, PointGeoDto> resoudrePositionsTempsReel(List<CapaciteEnAttente> capacites) {
        java.time.Instant maintenant = java.time.Instant.now();

        // UN appel HTTP groupe plutot que N appels sequentiels (plan de
        // reorientation, position GPS temps reel dans le matching - budget
        // L0 ~50ms incompatible avec un aller-retour reseau par vehicule
        // distinct du lot). Vehicules dedupliques avant l'appel : plusieurs
        // capacites peuvent partager le meme vehiculeId dans un cycle.
        List<java.util.UUID> vehiculeIdsDistincts = capacites.stream()
                .map(CapaciteEnAttente::getVehiculeId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        Map<java.util.UUID, PositionActuelleDto> positionsBrutes =
                serviceTrkClient.dernieresPositions(vehiculeIdsDistincts);

        Map<java.util.UUID, PointGeoDto> resultat = new java.util.HashMap<>();
        for (Map.Entry<java.util.UUID, PositionActuelleDto> entree : positionsBrutes.entrySet()) {
            java.util.UUID vehiculeId = entree.getKey();
            PositionActuelleDto position = entree.getValue();
            if (position == null) {
                continue; // vehicule absent/sans position connue cote TRK - deja logue cote ServiceTrkClient
            }
            long ageSecondes = java.time.Duration.between(position.horodatageCapture(), maintenant).getSeconds();
            if (ageSecondes > positionMaxAgeSecondes) {
                log.debug("Position temps reel du vehicule {} trop ancienne ({} s > seuil {} s) - "
                        + "repli sur la position declaree.", vehiculeId, ageSecondes, positionMaxAgeSecondes);
                continue;
            }
            resultat.put(vehiculeId, new PointGeoDto(position.latitude(), position.longitude()));
        }
        return resultat;
    }

    /**
     * Position effective d'une capacite pour ce cycle : temps reel si connue
     * et fraiche (positionsTempsReel), sinon repli sur la position declaree
     * (CapaciteEnAttente.getPosition()) - jamais un candidat traite comme
     * "position inconnue" simplement parce que TRK n'a pas encore recu de
     * PositionBrute pour lui (cas normal en debut de mission).
     */
    private PointGeoDto resoudrePositionEffective(CapaciteEnAttente capacite,
                                                   Map<java.util.UUID, PointGeoDto> positionsTempsReel) {
        java.util.UUID vehiculeId = capacite.getVehiculeId();
        if (vehiculeId != null) {
            PointGeoDto positionTempsReel = positionsTempsReel.get(vehiculeId);
            if (positionTempsReel != null) {
                return positionTempsReel;
            }
        }
        return capacite.getPosition();
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
     * Construit le CandidatCoutDto d'une paire demande/capacite avec les 7
     * criteres CDC S8.5.3 calcules pour CETTE paire (convention
     * DemandeAvecCandidats : valeurs deja normalisees par paire).
     */
    private CandidatCoutDto construireCandidatAvecCriteresCDC(DemandeEnAttente demande,
                                                               CapaciteEnAttente capacite,
                                                               java.time.Instant instantCalcul,
                                                               long fretAuRetour,
                                                               Double risqueAxeScore,
                                                               Map<java.util.UUID, PointGeoDto> positionsTempsReel) {
        return new CandidatCoutDto(capacite.getCapaciteId(), capacite.getTransporteurId(),
                capacite.getVehiculeId(),
                calculateurCoutSeptTermes.calculer(demande, capacite, instantCalcul,
                        fretAuRetour, risqueAxeScore),
                resoudrePositionEffective(capacite, positionsTempsReel),
                capacite.getProfilCamion(), capacite.getTypeVehicule());
    }

    /**
     * Filtrage L0 d'un axe de demandes : priorite a l'indexation H3 (cellule
     * H3 de l'origine + k anneaux, endpoints zonage cote GEO - CDC S8.5.4),
     * repli sur le rayon Haversine historique (RG-046) si GEO est injoignable.
     * Une position de candidat inconnue n'est JAMAIS exclue silencieusement
     * (ENF-DIS-04), quel que soit le mode de filtrage actif.
     *
     * @param cacheCellulesH3 cache par position du cycle courant : plusieurs
     *        demandes partagent souvent les memes positions de candidats,
     *        inutile de rappeler GEO pour chaque paire
     * @param h3Utilise sortie (tableau a une case, les lambdas ne mutent pas
     *        les locales) : true si au moins un filtrage H3 a reussi sur ce cycle
     */
    private List<CapaciteEnAttente> filtrerCandidatsL0(DemandeEnAttente demande,
                                                        List<CapaciteEnAttente> capacites,
                                                        Double rayonHaversineRepliKm,
                                                        Map<PointGeoDto, String> cacheCellulesH3,
                                                        boolean[] h3Utilise,
                                                        Map<java.util.UUID, PointGeoDto> positionsTempsReel) {
        if (capacites.isEmpty()) {
            return capacites;
        }
        PointGeoDto origine = demande.getOrigine();
        if (origine == null) {
            return filtrerCapacitePhysique(demande, capacites);
        }

        String celluleOrigine = serviceGeoClient.indexZonage(origine.latitude(), origine.longitude());
        List<CapaciteEnAttente> geospatiales;
        if (celluleOrigine != null) {
            List<String> cellulesEligibles = serviceGeoClient.kRing(celluleOrigine, l0KRing);
            if (!cellulesEligibles.isEmpty()) {
                h3Utilise[0] = true;
                geospatiales = capacites.stream()
                        .filter(c -> {
                            PointGeoDto position = resoudrePositionEffective(c, positionsTempsReel);
                            if (position == null) {
                                return true; // position inconnue : ne pas exclure silencieusement
                            }
                            String cellule = cacheCellulesH3.computeIfAbsent(position,
                                    p -> serviceGeoClient.indexZonage(p.latitude(), p.longitude()));
                            // GEO injoignable pour CE point : candidat conserve
                            // plutot qu'elimine par erreur de donnee manquante.
                            return cellule == null || cellulesEligibles.contains(cellule);
                        })
                        .toList();
                return filtrerCapacitePhysique(demande, geospatiales);
            }
        }

        // Repli historique : rayon Haversine autour de l'origine (RG-046,
        // "distance d'approche a vide" camion -> point de collecte).
        if (rayonHaversineRepliKm == null) {
            return filtrerCapacitePhysique(demande, capacites);
        }
        double rayonKm = rayonHaversineRepliKm;
        geospatiales = capacites.stream()
                .filter(c -> {
                    PointGeoDto position = resoudrePositionEffective(c, positionsTempsReel);
                    if (position == null) {
                        return true;
                    }
                    return HaversineUtils.distance(origine, position) <= rayonKm;
                })
                .toList();
        return filtrerCapacitePhysique(demande, geospatiales);
    }

    /**
     * Faisabilité physique (CDC §8.1 / EF-MAT-05) : un camion dont le reliquat
     * est inférieur au poids taxable de la demande ne doit jamais être proposé.
     * Sans ce filtre, L1 publie une PropositionEmise puis l'acceptation échoue
     * en 503 (decrement refuse cote service-cap).
     */
    static boolean capacitePhysiquementSuffisante(DemandeEnAttente demande, CapaciteEnAttente capacite) {
        if (demande.getPoidsTaxableKg() == null || capacite.getCapaciteResiduelleKg() == null) {
            return true;
        }
        return capacite.getCapaciteResiduelleKg().compareTo(demande.getPoidsTaxableKg()) >= 0;
    }

    private static List<CapaciteEnAttente> filtrerCapacitePhysique(DemandeEnAttente demande,
                                                                   List<CapaciteEnAttente> capacites) {
        return capacites.stream()
                .filter(c -> capacitePhysiquementSuffisante(demande, c))
                .toList();
    }

    /**
     * VALEUR_RETOUR (7e terme CDC S8.5.3, prospectif) : pour chaque demande,
     * nombre d'AUTRES demandes en attente dont l'ORIGINE est a moins de
     * fretcorridor.opt.matching.valeur-retour-rayon-km de SA destination -
     * proxy mesurable du "fret trouvable au retour". O(N^2) sur la file de
     * l'axe : negligeable aux volumes MVP (dizaines d'elements par axe/cycle).
     */
    private Map<java.util.UUID, Long> compterAutresDemandesProchesDestination(
            List<DemandeEnAttente> demandes) {
        double rayonKm = calculateurCoutSeptTermes.getValeurRetourRayonKm();
        Map<java.util.UUID, Long> compteParId = new java.util.HashMap<>();
        for (DemandeEnAttente d : demandes) {
            if (d.getDestination() == null) {
                continue;
            }
            long nbAutres = demandes.stream()
                    .filter(autre -> !autre.getDemandeId().equals(d.getDemandeId()))
                    .filter(autre -> autre.getOrigine() != null)
                    .filter(autre -> HaversineUtils.distance(d.getDestination(), autre.getOrigine()) <= rayonKm)
                    .count();
            compteParId.put(d.getDemandeId(), nbAutres);
        }
        return compteParId;
    }
}
