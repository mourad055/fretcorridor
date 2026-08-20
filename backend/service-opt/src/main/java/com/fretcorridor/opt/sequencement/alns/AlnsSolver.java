package com.fretcorridor.opt.sequencement.alns;

import com.fretcorridor.dto.PointGeoDto;
import com.fretcorridor.opt.domain.Affectation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Recherche a grand voisinage adaptative (CDC S8.6.2) - V1 : construction
 * gloutonne (chaque affectation inseree a sa meilleure position au fur et a
 * mesure, cf OperateurInsertion), suivie d'un nombre borne d'iterations
 * detruire/reconstruire. "Adaptive" simplifie en V1 a un seul couple
 * d'operateurs (retrait aleatoire + insertion moins cher realisable) -
 * extensible a plusieurs operateurs sans changer cette classe (cf javadoc
 * OperateurRetrait).
 *
 * Budget de latence CDC S8.10 (Sequencement L2) : cible P50 5s / P95 30s -
 * nbIterationsMax borne le temps de calcul, pas une garantie stricte de
 * respect du budget (a mesurer empiriquement, ajuster si depasse).
 */
@Component
public class AlnsSolver {

    private static final Logger log = LoggerFactory.getLogger(AlnsSolver.class);
    private static final int NB_ITERATIONS_MAX = 200;

    private final OperateurInsertion operateurInsertion;

    public AlnsSolver(OperateurInsertion operateurInsertion) {
        this.operateurInsertion = operateurInsertion;
    }

    public record ResultatSequencement(
            List<Affectation> affectationsInserees,
            List<Affectation> affectationsNonInserees,
            EtatSolution solutionFinale) {
    }

    /**
     * @param affectations   demandes/capacites a consolider sur une meme
     *                       capacite (cf SequencementDeclencheur)
     * @param capaciteMaxKg  plafond de la capacite (EF-CAP, deja connu du
     *                       porteur de la capacite - a transmettre par
     *                       l'appelant, cf point d'integration a completer)
     * @param parametresAxe  Axe.parametres (EF-GEO-02) - detourMaxDistanceKm,
     *                       coefficients RG-107
     */
    /**
     * @param chargeInitialeKg poids deja a bord au demarrage de cette
     *        recherche (Sprint 12, EF-MAT-09) - BigDecimal.ZERO pour une
     *        construction initiale, > 0 pour une replanification en cours
     *        de tournee (cf ReplanificationService).
     */
    public ResultatSequencement resoudre(List<Affectation> affectations, BigDecimal capaciteMaxKg,
                                          BigDecimal chargeInitialeKg, Map<String, Object> parametresAxe) {

        Map<UUID, PointGeoDto[]> positionsAffectations = affectations.stream()
                .collect(Collectors.toMap(Affectation::getId,
                        a -> new PointGeoDto[]{
                                new PointGeoDto(a.getOrigineLatitude(), a.getOrigineLongitude()),
                                new PointGeoDto(a.getDestinationLatitude(), a.getDestinationLongitude())
                        }));

        EtatSolution solution = new EtatSolution(capaciteMaxKg, chargeInitialeKg);
        java.util.List<Affectation> inserees = new java.util.ArrayList<>();
        java.util.List<Affectation> nonInserees = new java.util.ArrayList<>();

        // Construction initiale : insertion gloutonne, une affectation a la fois.
        for (Affectation affectation : affectations) {
            OperateurInsertion.ResultatInsertion resultat = operateurInsertion.trouverMeilleurePosition(
                    affectation, solution, positionsAffectations, parametresAxe);

            if (resultat != null) {
                solution = resultat.solution();
                inserees.add(affectation);
            } else {
                nonInserees.add(affectation);
            }
        }

        log.info("ALNS termine (construction initiale, V1 sans iterations destroy/repair) : "
                        + "{} inseree(s), {} non inseree(s) (capacite/detour)",
                inserees.size(), nonInserees.size());

        // TODO (increment suivant) : boucle destroy/repair (NB_ITERATIONS_MAX)
        // avec OperateurRetrait pour tenter de reinserer les affectations
        // non inserees via un reordonnancement - V1 se limite volontairement
        // a la construction initiale, deja fonctionnelle et testable seule
        // (module par module).

        return new ResultatSequencement(inserees, nonInserees, solution);
    }
}
