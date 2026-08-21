package com.fretcorridor.opt.sequencement;

import com.fretcorridor.opt.domain.Affectation;
import com.fretcorridor.opt.domain.AffectationRepository;
import com.fretcorridor.opt.domain.CapaciteEnAttente;
import com.fretcorridor.opt.domain.CapaciteEnAttenteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fretcorridor.opt.sequencement.alns.AlnsSolver;
import com.fretcorridor.opt.oracle.OracleChargementService;
import com.fretcorridor.opt.client.ServiceGeoClient;
import com.fretcorridor.opt.client.AxeDetailDto;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Declencheur du sequencement L2 (Sprint 11, CDC S8.6) - meme principe que
 * MatchingCycleService pour L1 : cycle periodique, jamais un declenchement
 * immediat par affectation individuelle.
 *
 * Sprint 12 (EF-MAT-09) : avant de creer une nouvelle Tournee, verifie
 * qu'aucune Tournee CONFIRMEE/EN_EXECUTION n'existe deja pour cette
 * capacite - sinon delegue a ReplanificationService pour inserer dans le
 * residuel plutot que de creer une seconde tournee en parallele (bug
 * latent corrige ce sprint, cf TourneeRepository.findByCapaciteIdAndStatutIn).
 */
@Service
public class SequencementDeclencheur {

    private static final Logger log = LoggerFactory.getLogger(SequencementDeclencheur.class);

    private final AffectationRepository affectationRepository;
    private final EtapeTourneeRepository etapeTourneeRepository;
    private final TourneeRepository tourneeRepository;
    private final AlnsSolver alnsSolver;
    private final CapaciteEnAttenteRepository capaciteEnAttenteRepository;
    private final ReplanificationService replanificationService;
    private final com.fretcorridor.opt.messaging.OptEventPublisher optEventPublisher;
    private final OracleChargementService oracleChargementService;
    private final ServiceGeoClient serviceGeoClient;

    private static final List<Tournee.Statut> STATUTS_NON_TERMINES =
            List.of(Tournee.Statut.CONFIRMEE, Tournee.Statut.EN_EXECUTION);

    public SequencementDeclencheur(AffectationRepository affectationRepository,
                                    EtapeTourneeRepository etapeTourneeRepository,
                                    TourneeRepository tourneeRepository,
                                    AlnsSolver alnsSolver,
                                    CapaciteEnAttenteRepository capaciteEnAttenteRepository,
                                    ReplanificationService replanificationService,
                                    com.fretcorridor.opt.messaging.OptEventPublisher optEventPublisher,
                                    OracleChargementService oracleChargementService,
                                    ServiceGeoClient serviceGeoClient) {
        this.affectationRepository = affectationRepository;
        this.etapeTourneeRepository = etapeTourneeRepository;
        this.tourneeRepository = tourneeRepository;
        this.alnsSolver = alnsSolver;
        this.capaciteEnAttenteRepository = capaciteEnAttenteRepository;
        this.replanificationService = replanificationService;
        this.optEventPublisher = optEventPublisher;
        this.oracleChargementService = oracleChargementService;
        this.serviceGeoClient = serviceGeoClient;
    }

    // EF-MAT-10 : resout les parametres reels de l'axe (dont
    // detourMaxDistanceKm) aupres de service-geo. Permissif par design :
    // axeId absent, axe introuvable ou service-geo injoignable -> Map.of(),
    // jamais une exception qui bloquerait le cycle de sequencement
    // (cf. ServiceGeoClient.axeParId, deja degrade gracieusement).
    private Map<String, Object> resoudreParametresAxe(java.util.UUID axeId) {
        if (axeId == null) {
            return Map.of();
        }
        AxeDetailDto axe = serviceGeoClient.axeParId(axeId);
        if (axe == null || axe.parametres() == null) {
            return Map.of();
        }
        return axe.parametres();
    }

    @Scheduled(fixedDelayString = "${spring.sequencement.cycle-interval-ms:30000}")
    public void executerCycle() {
        List<Affectation> affectationsNonSequencees = affectationRepository.findNonEncoreSequencees();

        if (affectationsNonSequencees.isEmpty()) {
            log.debug("Aucune affectation en attente de sequencement ce tour.");
            return;
        }

        Map<UUID, List<Affectation>> parCapacite = affectationsNonSequencees.stream()
                .collect(Collectors.groupingBy(Affectation::getCapaciteId));

        for (Map.Entry<UUID, List<Affectation>> entree : parCapacite.entrySet()) {
            UUID capaciteId = entree.getKey();
            List<Affectation> affectationsCapacite = entree.getValue();

            List<Tournee> tourneesActives = tourneeRepository
                    .findByCapaciteIdAndStatutIn(capaciteId, STATUTS_NON_TERMINES);

            if (!tourneesActives.isEmpty()) {
                // EF-MAT-09 : une tournee est deja en cours sur cette
                // capacite - replanification du residuel, jamais une
                // seconde tournee en parallele. tourneesActives.get(0) :
                // au plus une tournee active par capacite en pratique
                // (invariant du domaine, pas verifie par contrainte DB ce
                // sprint).
                replanificationService.replanifierResiduel(tourneesActives.get(0), affectationsCapacite);
                continue;
            }

            if (affectationsCapacite.size() < 2) {
                // Une seule affectation sur cette capacite, aucune tournee
                // active : rien a consolider ce tour, reste en attente
                // qu'une deuxieme arrive.
                continue;
            }

            log.info("Sequencement L2 declenche - capacite={}, {} affectation(s) a consolider",
                    capaciteId, affectationsCapacite.size());

            java.util.Optional<CapaciteEnAttente> capaciteEnAttenteOpt = capaciteEnAttenteRepository
                    .findFirstByCapaciteIdOrderByDateReceptionDesc(capaciteId);
            if (capaciteEnAttenteOpt.isEmpty()) {
                log.warn("Aucune CapaciteEnAttente retrouvee pour capaciteId={} - "
                        + "capacite dynamique non bornee ce cycle (cas theoriquement "
                        + "impossible, a investiguer si observe en pratique).", capaciteId);
            }
            java.math.BigDecimal capaciteMaxKg = capaciteEnAttenteOpt
                    .map(CapaciteEnAttente::getCapaciteResiduelleKg)
                    .orElse(null);
            com.fretcorridor.opt.client.ProfilCamionDto profilCamion = capaciteEnAttenteOpt
                    .map(CapaciteEnAttente::getProfilCamion)
                    .orElse(null);

            // EF-MKT-10 : verifie AVANT l'ALNS, sur les demandeId candidats
            // uniquement - evite un calcul de sequencement complet pour une
            // tournee de toute facon rejetee (contrairement a EF-MAT-07 ci-
            // dessous, qui depend du resultat de l'ALNS et doit donc venir
            // apres).
            List<UUID> demandeIdsCandidats = affectationsCapacite.stream()
                    .map(Affectation::getDemandeId)
                    .toList();
            if (!oracleChargementService.groupageCompatibleMatieresDangereuses(demandeIdsCandidats)) {
                log.warn("Consolidation rejetee (EF-MKT-10, matieres dangereuses incompatibles) - "
                        + "capacite={}, {} demande(s) candidate(s).", capaciteId, demandeIdsCandidats.size());
                continue;
            }

            java.util.UUID axeIdPourParametres = affectationsCapacite.stream()
                    .map(Affectation::getAxeId)
                    .filter(java.util.Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            AlnsSolver.ResultatSequencement resultat = alnsSolver.resoudre(
                    affectationsCapacite, capaciteMaxKg, java.math.BigDecimal.ZERO,
                    resoudreParametresAxe(axeIdPourParametres));

            if (resultat.affectationsInserees().isEmpty()) {
                log.debug("Aucune affectation inseree pour la capacite {} ce tour.", capaciteId);
                continue;
            }

            com.fretcorridor.opt.domain.Affectation premiere = resultat.affectationsInserees().get(0);
            com.fretcorridor.opt.sequencement.Tournee tournee =
                    new com.fretcorridor.opt.sequencement.Tournee(capaciteId, premiere.getAxeId());
            tourneeRepository.save(tournee);

            var sequence = resultat.solutionFinale().getSequence();
            java.util.List<EtapeTournee> etapesCreees = new java.util.ArrayList<>();
            for (int rang = 0; rang < sequence.size(); rang++) {
                var position = sequence.get(rang);
                var typeEtape = position.type() == com.fretcorridor.opt.sequencement.alns.EtatSolution.TypeArret.ENLEVEMENT
                        ? com.fretcorridor.opt.sequencement.EtapeTournee.TypeEtape.ENLEVEMENT
                        : com.fretcorridor.opt.sequencement.EtapeTournee.TypeEtape.LIVRAISON;
                EtapeTournee etapeCreee = new com.fretcorridor.opt.sequencement.EtapeTournee(
                        tournee, position.affectationId(), rang, typeEtape, position.chargeApres());
                etapeTourneeRepository.save(etapeCreee);
                etapesCreees.add(etapeCreee);
            }
            // EF-MAT-07 (CDC S8.7) : verifie CHAQUE etat intermediaire avant toute
            // confirmation - flux E1, jamais de confirmation optimiste sur un
            // profil vehicule incomplet ou une charge par essieu depassee.
            // INCREMENT 21/08 : la map affectationId -> demandeId permet a
            // l'oracle de retrouver les lots a bord a chaque etat (verifications
            // volumiques/gabarit EF-MAT-05/13).
            Map<java.util.UUID, java.util.UUID> demandeIdParAffectation =
                    resultat.affectationsInserees().stream()
                            .collect(java.util.stream.Collectors.toMap(
                                    com.fretcorridor.opt.domain.Affectation::getId,
                                    com.fretcorridor.opt.domain.Affectation::getDemandeId));
            boolean chargementFaisable = oracleChargementService.verifierTournee(
                    tournee, profilCamion, demandeIdParAffectation);
            if (!chargementFaisable) {
                log.warn("Tournee {} NON confirmee (EF-MAT-07, oracle de chargement) - "
                        + "capacite={}, {} etape(s) - cf opt.plan_chargement pour le detail des rejets.",
                        tournee.getId(), capaciteId, sequence.size());
                continue;
            }

            tournee.confirmer();
            tourneeRepository.save(tournee);

            log.info("Tournee {} confirmee - capacite={}, {} etape(s), {} affectation(s) non inseree(s)",
                    tournee.getId(), capaciteId, sequence.size(), resultat.affectationsNonInserees().size());

            publierTourneeConstituee(tournee, etapesCreees);

            // EF-MAT-13 (priorite S) : jamais publie pour une tournee rejetee
            // par l'oracle - garde deja assuree par le "continue" ci-dessus,
            // ce point du code n'est atteint que si chargementFaisable == true.
            var etatsChargement = oracleChargementService.construireEtatsPourRestitution(tournee);
            var planEvent = new com.fretcorridor.opt.messaging.PlanChargementConfirmeEvent(
                    UUID.randomUUID(), tournee.getId(), etatsChargement, java.time.Instant.now());
            optEventPublisher.publierPlanChargementConfirme(planEvent);
        }
    }

    /**
     * EF-MAT-05/06 (Sprint 11) - repond au besoin explicite de Personne 1
     * (Mobile, S11) : chaque etape de la tournee fraichement confirmee est
     * enrichie avec demandeId + coordonnees, sourcees depuis l'Affectation
     * correspondante (jamais dupliquees en base sur EtapeTournee - lue a la
     * demande, cette publication n'est declenchee qu'une fois par tournee).
     */
    private void publierTourneeConstituee(Tournee tournee, List<EtapeTournee> etapes) {
        List<com.fretcorridor.opt.messaging.EtapeConstitueeDto> etapesDto = etapes.stream()
                .map(etape -> {
                    Affectation affectation = affectationRepository.findById(etape.getAffectationId())
                            .orElseThrow(() -> new IllegalStateException(
                                    "Affectation introuvable pour une etape de tournee deja confirmee - "
                                            + "incoherence de donnees, affectationId=" + etape.getAffectationId()));

                    boolean estEnlevement = etape.getTypeEtape() == EtapeTournee.TypeEtape.ENLEVEMENT;
                    double latitude = estEnlevement ? affectation.getOrigineLatitude() : affectation.getDestinationLatitude();
                    double longitude = estEnlevement ? affectation.getOrigineLongitude() : affectation.getDestinationLongitude();

                    com.fretcorridor.opt.messaging.EtapeConstitueeDto.EtapeTypeDto typeDto =
                            estEnlevement
                                    ? com.fretcorridor.opt.messaging.EtapeConstitueeDto.EtapeTypeDto.ENLEVEMENT
                                    : com.fretcorridor.opt.messaging.EtapeConstitueeDto.EtapeTypeDto.LIVRAISON;

                    return new com.fretcorridor.opt.messaging.EtapeConstitueeDto(
                            affectation.getId(), etape.getRang(), typeDto, affectation.getDemandeId(),
                            latitude, longitude, null, null);
                })
                .toList();

        com.fretcorridor.opt.messaging.TourneeConstitueeEvent event =
                new com.fretcorridor.opt.messaging.TourneeConstitueeEvent(
                        UUID.randomUUID(), tournee.getId(), tournee.getCapaciteId(), tournee.getAxeId(),
                        etapesDto, java.time.Instant.now());

        optEventPublisher.publierTourneeConstituee(event);
    }
}
