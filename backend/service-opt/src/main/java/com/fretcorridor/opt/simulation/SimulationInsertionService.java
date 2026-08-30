package com.fretcorridor.opt.simulation;

import com.fretcorridor.dto.PointGeoDto;
import com.fretcorridor.opt.client.AxeDetailDto;
import com.fretcorridor.opt.client.ItineraireRequestDto;
import com.fretcorridor.opt.client.ItineraireResponseDto;
import com.fretcorridor.opt.client.ServiceGeoClient;
import com.fretcorridor.opt.client.ValhallaClient;
import com.fretcorridor.opt.domain.Affectation;
import com.fretcorridor.opt.domain.AffectationRepository;
import com.fretcorridor.opt.domain.CapaciteEnAttente;
import com.fretcorridor.opt.domain.CapaciteEnAttenteRepository;
import com.fretcorridor.opt.sequencement.EtapeTournee;
import com.fretcorridor.opt.sequencement.EtapeTourneeRepository;
import com.fretcorridor.opt.sequencement.Tournee;
import com.fretcorridor.opt.sequencement.TourneeRepository;
import com.fretcorridor.opt.sequencement.alns.AlnsSolver;
import com.fretcorridor.opt.sequencement.alns.EtatSolution;
import com.fretcorridor.opt.web.dto.SimulationInsertionRequest;
import com.fretcorridor.opt.web.dto.SimulationInsertionResponse;
import com.fretcorridor.util.HaversineUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Point 4 du plan de reorientation : endpoint ON-DEMAND (pas cyclique) qui
 * simule l'insertion d'une nouvelle demande dans la tournee EN COURS du
 * chauffeur, SANS rien committer (dry-run totalement in-memory).
 *
 * S'appuie sur l'operateur/le solveur ALNS existant (Sprint 11,
 * AlnsSolver.resoudre) en mode revision : on calcule deux scenarios -
 * tournee seule vs tournee + nouvelle demande - puis on restitue le detour
 * (km) et le temps ajoute (Valhalla). Aucune entite n'est construite en
 * base : l'Affectation de la demande simulee est factice (id par reflexion,
 * comme dans AlnsSolverTest) et jamais sauvegardee.
 *
 * Ne modifie jamais la tournee : toutes les lectures sont transactionnelles
 * en lecture seule, toutes les ecritures sont absentes.
 */
@Service
public class SimulationInsertionService {

    private static final Logger log = LoggerFactory.getLogger(SimulationInsertionService.class);

    private final TourneeRepository tourneeRepository;
    private final EtapeTourneeRepository etapeTourneeRepository;
    private final AffectationRepository affectationRepository;
    private final CapaciteEnAttenteRepository capaciteEnAttenteRepository;
    private final AlnsSolver alnsSolver;
    private final ServiceGeoClient serviceGeoClient;
    private final ValhallaClient valhallaClient;

    public SimulationInsertionService(TourneeRepository tourneeRepository,
                                      EtapeTourneeRepository etapeTourneeRepository,
                                      AffectationRepository affectationRepository,
                                      CapaciteEnAttenteRepository capaciteEnAttenteRepository,
                                      AlnsSolver alnsSolver,
                                      ServiceGeoClient serviceGeoClient,
                                      ValhallaClient valhallaClient) {
        this.tourneeRepository = tourneeRepository;
        this.etapeTourneeRepository = etapeTourneeRepository;
        this.affectationRepository = affectationRepository;
        this.capaciteEnAttenteRepository = capaciteEnAttenteRepository;
        this.alnsSolver = alnsSolver;
        this.serviceGeoClient = serviceGeoClient;
        this.valhallaClient = valhallaClient;
    }

    public SimulationInsertionResponse simulerInsertion(SimulationInsertionRequest requete) {
        List<Tournee> tournees = tourneeRepository.findByCapaciteIdAndStatutIn(
                requete.capaciteId(),
                List.of(Tournee.Statut.CONFIRMEE, Tournee.Statut.EN_EXECUTION));

        if (tournees.isEmpty()) {
            log.info("Simulation insertion : aucune tournee en cours pour la capacite {} - "
                    + "la demande serait traitee en premiere insertion.", requete.capaciteId());
            return simulerSansTournee(requete);
        }

        // En theorie une seule tournee active par capacite (invariant Sprint 12).
        Tournee tournee = tournees.get(0);

        List<EtapeTournee> etapes = etapeTourneeRepository.findByTourneeIdOrderByRangAsc(tournee.getId());
        List<UUID> affectationIds = etapes.stream()
                .map(EtapeTournee::getAffectationId)
                .distinct()
                .toList();

        List<Affectation> affectationsTournee = affectationIds.isEmpty()
                ? List.of()
                : affectationRepository.findAllById(affectationIds);

        Affectation nouvelleDemande = construireAffectationFactice(requete);

        List<Affectation> tourneePlusNouvelle = new ArrayList<>(affectationsTournee);
        tourneePlusNouvelle.add(nouvelleDemande);

        Map<String, Object> parametresAxe = resoudreParametresAxe(requete.axeId());
        BigDecimal capaciteMaxKg = capaciteEnAttenteRepository
                .findFirstByCapaciteIdOrderByDateReceptionDesc(requete.capaciteId())
                .map(CapaciteEnAttente::getCapaciteResiduelleKg)
                .orElse(null);

        AlnsSolver.ResultatSequencement scA = alnsSolver.resoudre(
                affectationsTournee, capaciteMaxKg, BigDecimal.ZERO, parametresAxe);
        AlnsSolver.ResultatSequencement scB = alnsSolver.resoudre(
                tourneePlusNouvelle, capaciteMaxKg, BigDecimal.ZERO, parametresAxe);

        Map<UUID, PointGeoDto[]> positions = positionsDe(scB);
        double distanceA = distanceDe(scA.solutionFinale(), positions, requete);
        double distanceB = distanceDe(scB.solutionFinale(), positions, requete);
        double detourKm = Math.max(0.0, distanceB - distanceA);

        boolean inseree = scB.affectationsInserees().stream()
                .anyMatch(a -> a.getId().equals(nouvelleDemande.getId()));

        Double dureeAjoutee = null;
        if (inseree && !scB.solutionFinale().estVide()) {
            var iti = valhallaClient.calculerItineraire(construireRequeteValhalla(scB.solutionFinale(), positions));
            if (iti != null) {
                dureeAjoutee = iti.dureeSecondes();
            }
        }

        double tourneeKm = distanceB;
        Double tourneeDuree = (inseree && !scB.solutionFinale().estVide())
                ? dureeAjoutee
                : null;

        log.info("Simulation insertion (dry-run) - capacite={}, tournee={}, inseree={}, "
                        + "detourKm={}, tempsAjoute={}s (avant tourneeKm={}km, apres={}km)",
                requete.capaciteId(), tournee.getId(), inseree,
                String.format("%.2f", detourKm),
                dureeAjoutee == null ? "null" : String.format("%.0f", dureeAjoutee),
                String.format("%.2f", distanceA), String.format("%.2f", distanceB));

        return new SimulationInsertionResponse(inseree, detourKm, dureeAjoutee, tourneeKm, tourneeDuree);
    }

    /**
     * Cas sans tournee en cours : la nouvelle demande serait la premiere
     * mission du vehicule. Detour = 0 (rien n'existe encore), on ne restitue
     * que l'itineraire de cette seule demande.
     */
    private SimulationInsertionResponse simulerSansTournee(SimulationInsertionRequest requete) {
        Affectation seule = construireAffectationFactice(requete);
        Map<UUID, PointGeoDto[]> positions = Map.of(
                seule.getId(), new PointGeoDto[]{
                        new PointGeoDto(requete.origineLatitude(), requete.origineLongitude()),
                        new PointGeoDto(requete.destinationLatitude(), requete.destinationLongitude())
                });

        EtatSolution solution = new EtatSolution(null);
        EtatSolution resolu = solution.avecInsertion(seule, 0, 0);
        double km = resolu == null ? 0.0 : distanceDe(resolu, positions, requete);

        Double duree = null;
        if (resolu != null) {
            var iti = valhallaClient.calculerItineraire(construireRequeteValhalla(resolu, positions));
            if (iti != null) {
                duree = iti.dureeSecondes();
            }
        }

        log.info("Simulation insertion (sans tournee) - capacite={}, km={}, duree={}s",
                requete.capaciteId(), String.format("%.2f", km),
                duree == null ? "null" : String.format("%.0f", duree));

        return new SimulationInsertionResponse(true, 0.0, duree, km, duree);
    }

    // --- Helpers ---

    private Map<UUID, PointGeoDto[]> positionsDe(AlnsSolver.ResultatSequencement resultat) {
        Map<UUID, PointGeoDto[]> map = new java.util.HashMap<>();
        for (var a : resultat.affectationsInserees()) {
            map.put(a.getId(), new PointGeoDto[]{
                    new PointGeoDto(a.getOrigineLatitude(), a.getOrigineLongitude()),
                    new PointGeoDto(a.getDestinationLatitude(), a.getDestinationLongitude())
            });
        }
        for (var a : resultat.affectationsNonInserees()) {
            map.put(a.getId(), new PointGeoDto[]{
                    new PointGeoDto(a.getOrigineLatitude(), a.getOrigineLongitude()),
                    new PointGeoDto(a.getDestinationLatitude(), a.getDestinationLongitude())
            });
        }
        return map;
    }

    /**
     * Distance Haversine totale (km) parcourue par la sequence complete : on
     * enfile les points d'arret dans l'ordre, en resolvant chaque position
     * soit depuis la map deja construite, soit depuis la demande simulee
     * (positionsDemande).
     */
    private double distanceDe(EtatSolution solution, Map<UUID, PointGeoDto[]> positions,
                              SimulationInsertionRequest requete) {
        List<EtatSolution.PositionPlanifiee> sequence = solution.getSequence();
        if (sequence.isEmpty()) {
            return 0.0;
        }
        PointGeoDto avant = new PointGeoDto(requete.origineLatitude(), requete.origineLongitude());
        double total = 0.0;
        for (EtatSolution.PositionPlanifiee position : sequence) {
            PointGeoDto point = resoudre(position, positions, requete);
            total += HaversineUtils.distance(avant, point);
            avant = point;
        }
        return total;
    }

    private PointGeoDto resoudre(EtatSolution.PositionPlanifiee position,
                                 Map<UUID, PointGeoDto[]> positions,
                                 SimulationInsertionRequest requete) {
        PointGeoDto[] paire = positions.get(position.affectationId());
        if (paire != null) {
            return position.type() == EtatSolution.TypeArret.ENLEVEMENT ? paire[0] : paire[1];
        }
        return position.type() == EtatSolution.TypeArret.ENLEVEMENT
                ? new PointGeoDto(requete.origineLatitude(), requete.origineLongitude())
                : new PointGeoDto(requete.destinationLatitude(), requete.destinationLongitude());
    }

    private ItineraireRequestDto construireRequeteValhalla(EtatSolution solution,
                                                           Map<UUID, PointGeoDto[]> positions) {
        List<PointGeoDto> points = new ArrayList<>();
        for (EtatSolution.PositionPlanifiee position : solution.getSequence()) {
            PointGeoDto[] paire = positions.get(position.affectationId());
            if (paire != null) {
                points.add(position.type() == EtatSolution.TypeArret.ENLEVEMENT ? paire[0] : paire[1]);
            }
        }
        if (points.isEmpty()) {
            points.add(new PointGeoDto(0.0, 0.0));
        }
        return new ItineraireRequestDto(points, null);
    }

    private Map<String, Object> resoudreParametresAxe(UUID axeId) {
        if (axeId == null) {
            return Map.of();
        }
        AxeDetailDto axe = serviceGeoClient.axeParId(axeId);
        if (axe == null || axe.parametres() == null) {
            return Map.of();
        }
        return axe.parametres();
    }

    /**
     * Construit une Affectation FACTICE (jamais sauvegardee) representant la
     * demande simulee, uniquement pour alimenter l'ALNS en dry-run. L'id est
     * pose par reflexion (comme AlnsSolverTest) car le constructeur public ne
     * l'expose pas - strictement in-memory, aucun cycle de vie JPA.
     */
    private Affectation construireAffectationFactice(SimulationInsertionRequest requete) {
        Affectation affectation = new Affectation(
                UUID.randomUUID(), requete.capaciteId(), null, null, requete.axeId(),
                requete.poidsKg() != null ? requete.poidsKg() : BigDecimal.ZERO,
                requete.origineLatitude(), requete.origineLongitude(),
                requete.destinationLatitude(), requete.destinationLongitude(),
                null, null, null, null,
                BigDecimal.TEN,
                null, null, null,
                null, null, null, null,
                null, null,
                null, null, null,
                requete.matieresDangereuses(),
                null, null, null, null, null, null, null, null, null
        );
        try {
            Field idField = Affectation.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(affectation, UUID.randomUUID());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.warn("Impossible de poser l'id factice de l'affectation simulee - simulation degradee : {}",
                    e.getMessage());
        }
        return affectation;
    }
}
