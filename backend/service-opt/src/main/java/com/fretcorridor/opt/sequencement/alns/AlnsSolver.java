package com.fretcorridor.opt.sequencement.alns;

import com.fretcorridor.dto.PointGeoDto;
import com.fretcorridor.opt.domain.Affectation;
import com.fretcorridor.util.HaversineUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Recherche a grand voisinage adaptative (CDC S8.6.2) - V2 : construction
 * gloutonne initiale, suivie d'une VRAIE boucle destroy/repair avec critere
 * d'acceptation de type recuit simule, conforme au principe du CDC :
 * "detruire partiellement une solution par un operateur de retrait, la
 * reconstruire par un operateur d'insertion, accepter selon un critere de
 * type recuit, et adapter dynamiquement la probabilite de selection des
 * operateurs selon leur succes passe."
 *
 * CE QUI EST REEL dans cette version :
 *  - Destroy (OperateurRetrait, retrait aleatoire) + repair (OperateurInsertion,
 *    insertion moins cher realisable) en boucle sur NB_ITERATIONS_MAX iterations.
 *  - Acceptation type recuit simule (accepte toujours une solution meilleure,
 *    accepte une solution pire avec une probabilite decroissante avec la
 *    temperature, cf accepterSelonRecuit) - permet d'echapper aux optima
 *    locaux ou une construction gloutonne pure resterait bloquee.
 *  - La solution retournee est la MEILLEURE trouvee sur l'ensemble des
 *    iterations (elitisme), jamais simplement l'etat courant final du recuit
 *    (qui peut etre transitoirement pire).
 *
 * CE QUI RESTE UNE SIMPLIFICATION ASSUMEE (TODO increment suivant), a ne
 * jamais presenter comme fait :
 *  - "Adaptive" au sens strict du CDC suppose PLUSIEURS operateurs de
 *    retrait/insertion parmi lesquels la probabilite de selection s'adapte
 *    selon leur succes passe. V2 n'a qu'un seul couple retrait/insertion -
 *    rien a adapter tant qu'il n'y a qu'un choix.
 *  - Le cout d'objectif utilise ici pour l'acceptation (distance totale +
 *    penalite forte par affectation non inseree, meme principe que Ropke &
 *    Pisinger 2006 cite en bibliographie CDC) est une approximation
 *    d'ingenierie du front de compromis RG-107 (S8.6.3) - PAS la somme
 *    ponderee configurable par axe/tenant (celle-ci s'applique au niveau
 *    du cout composite L1/MAT, pas au sequencement interne L2).
 *
 * Budget de latence CDC S8.10 (Sequencement L2) : cible P50 5s / P95 30s -
 * NB_ITERATIONS_MAX borne le temps de calcul, pas une garantie stricte (a
 * mesurer empiriquement).
 */
@Component
public class AlnsSolver {

    private static final Logger log = LoggerFactory.getLogger(AlnsSolver.class);

    private static final int NB_ITERATIONS_MAX = 200;

    // Parametres du recuit simule - choix d'ingenierie du solveur (au meme
    // titre que NB_ITERATIONS_MAX), pas une regle metier au sens RG-* du
    // CDC : ne relevent pas du meme principe "jamais code en dur" applique
    // a Axe.parametres (qui concerne des donnees de gestion, pas des
    // hyperparametres d'algorithme).
    private static final double TEMPERATURE_INITIALE_KM = 50.0;
    private static final double TAUX_REFROIDISSEMENT = 0.95;

    // Penalite forte par affectation non inseree dans le cout d'objectif du
    // recuit - garantit qu'inserer une affectation supplementaire est
    // (quasi) toujours prefere a n'importe quelle economie de distance
    // seule. Meme principe que Ropke & Pisinger (2006, bibliographie CDC)
    // pour les demandes non servies.
    private static final double PENALITE_NON_INSEREE_KM = 10_000.0;

    private final OperateurInsertion operateurInsertion;
    private final OperateurRetrait operateurRetrait;
    private final Random random;

    public AlnsSolver(OperateurInsertion operateurInsertion) {
        this.operateurInsertion = operateurInsertion;
        this.random = new Random();
        // OperateurRetrait n'est pas un bean Spring (prend un Random en
        // constructeur) - instancie ici, seul point d'utilisation actuel.
        this.operateurRetrait = new OperateurRetrait(this.random);
    }

    public record ResultatSequencement(
            List<Affectation> affectationsInserees,
            List<Affectation> affectationsNonInserees,
            EtatSolution solutionFinale) {
    }

    /**
     * @param affectations   demandes/capacites a consolider sur une meme
     *                       capacite (cf SequencementDeclencheur)
     * @param capaciteMaxKg  plafond de la capacite (EF-CAP)
     * @param chargeInitialeKg poids deja a bord au demarrage de cette
     *        recherche (Sprint 12, EF-MAT-09)
     * @param parametresAxe  Axe.parametres (EF-GEO-02) - detourMaxDistanceKm,
     *                       coefficients RG-107
     */
    public ResultatSequencement resoudre(List<Affectation> affectations, BigDecimal capaciteMaxKg,
                                          BigDecimal chargeInitialeKg, Map<String, Object> parametresAxe) {

        Map<UUID, Affectation> affectationParId = affectations.stream()
                .collect(Collectors.toMap(Affectation::getId, a -> a));

        Map<UUID, PointGeoDto[]> positionsAffectations = affectations.stream()
                .collect(Collectors.toMap(Affectation::getId,
                        a -> new PointGeoDto[]{
                                new PointGeoDto(a.getOrigineLatitude(), a.getOrigineLongitude()),
                                new PointGeoDto(a.getDestinationLatitude(), a.getDestinationLongitude())
                        }));

        EtatSolution solution = new EtatSolution(capaciteMaxKg, chargeInitialeKg);
        List<Affectation> inserees = new ArrayList<>();
        List<Affectation> nonInserees = new ArrayList<>();

        // --- Construction initiale : insertion gloutonne (inchangee) ---
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

        log.debug("ALNS - construction initiale : {} inseree(s), {} non inseree(s) avant destroy/repair",
                inserees.size(), nonInserees.size());

        // --- Boucle destroy/repair avec acceptation type recuit simule ---
        double coutCourant = calculerCoutSolution(solution, nonInserees, positionsAffectations);

        EtatSolution meilleureSolution = solution;
        List<Affectation> meilleuresInserees = new ArrayList<>(inserees);
        List<Affectation> meilleuresNonInserees = new ArrayList<>(nonInserees);
        double meilleurCout = coutCourant;

        double temperature = TEMPERATURE_INITIALE_KM;

        for (int iteration = 0; iteration < NB_ITERATIONS_MAX; iteration++) {

            List<UUID> idsDansSolution = solution.getSequence().stream()
                    .map(EtatSolution.PositionPlanifiee::affectationId)
                    .distinct()
                    .toList();

            if (idsDansSolution.isEmpty()) {
                break; // rien a detruire - inutile de continuer
            }

            int nbARetirer = 1 + random.nextInt(Math.min(3, idsDansSolution.size()));
            List<UUID> idsRetires = operateurRetrait.selectionnerPourRetrait(idsDansSolution, nbARetirer);
            Set<UUID> idsRetiresSet = new HashSet<>(idsRetires);

            EtatSolution solutionDetruite = solution.sansAffectations(idsRetiresSet);

            // Candidats a reinserer ce tour : les affectations retirees +
            // celles deja non inserees jusqu'ici - chance de trouver une
            // meilleure position maintenant que la sequence a change
            // (impossible avec la seule construction gloutonne, qui ne
            // revisite jamais une decision passee).
            List<Affectation> aReinserer = new ArrayList<>();
            idsRetires.forEach(id -> aReinserer.add(affectationParId.get(id)));
            aReinserer.addAll(nonInserees);
            java.util.Collections.shuffle(aReinserer, random);

            List<Affectation> inserreesCandidate = idsDansSolution.stream()
                    .filter(id -> !idsRetiresSet.contains(id))
                    .map(affectationParId::get)
                    .collect(Collectors.toCollection(ArrayList::new));
            List<Affectation> nonInsereesCandidate = new ArrayList<>();
            EtatSolution solutionReconstruite = solutionDetruite;

            for (Affectation affectation : aReinserer) {
                OperateurInsertion.ResultatInsertion resultat = operateurInsertion.trouverMeilleurePosition(
                        affectation, solutionReconstruite, positionsAffectations, parametresAxe);
                if (resultat != null) {
                    solutionReconstruite = resultat.solution();
                    inserreesCandidate.add(affectation);
                } else {
                    nonInsereesCandidate.add(affectation);
                }
            }

            double coutCandidat = calculerCoutSolution(solutionReconstruite, nonInsereesCandidate, positionsAffectations);

            if (accepterSelonRecuit(coutCourant, coutCandidat, temperature)) {
                solution = solutionReconstruite;
                inserees.clear();
                inserees.addAll(inserreesCandidate);
                nonInserees.clear();
                nonInserees.addAll(nonInsereesCandidate);
                coutCourant = coutCandidat;

                if (coutCandidat < meilleurCout) {
                    meilleurCout = coutCandidat;
                    meilleureSolution = solutionReconstruite;
                    meilleuresInserees = new ArrayList<>(inserreesCandidate);
                    meilleuresNonInserees = new ArrayList<>(nonInsereesCandidate);
                }
            }
            // Solution rejetee : etat courant inchange, la prochaine
            // iteration repart de la, pas de la tentative rejetee.

            temperature *= TAUX_REFROIDISSEMENT;
        }

        log.info("ALNS termine ({} iteration(s) destroy/repair) : {} inseree(s), {} non inseree(s) "
                        + "(meilleur cout={} km-equivalent)",
                NB_ITERATIONS_MAX, meilleuresInserees.size(), meilleuresNonInserees.size(), meilleurCout);

        return new ResultatSequencement(meilleuresInserees, meilleuresNonInserees, meilleureSolution);
    }

    /**
     * Cout d'objectif interne au recuit ALNS : distance totale parcourue par
     * la sequence + penalite forte par affectation non inseree (cf javadoc
     * de classe - approximation d'ingenierie, pas la somme ponderee RG-107).
     */
    private double calculerCoutSolution(EtatSolution solution, List<Affectation> nonInserees,
                                         Map<UUID, PointGeoDto[]> positionsAffectations) {
        List<EtatSolution.PositionPlanifiee> sequence = solution.getSequence();

        double distanceTotaleKm = 0.0;
        PointGeoDto pointPrecedent = null;
        for (EtatSolution.PositionPlanifiee position : sequence) {
            PointGeoDto[] paire = positionsAffectations.get(position.affectationId());
            PointGeoDto pointCourant = position.type() == EtatSolution.TypeArret.ENLEVEMENT ? paire[0] : paire[1];
            if (pointPrecedent != null) {
                distanceTotaleKm += HaversineUtils.distance(pointPrecedent, pointCourant);
            }
            pointPrecedent = pointCourant;
        }

        return distanceTotaleKm + nonInserees.size() * PENALITE_NON_INSEREE_KM;
    }

    /**
     * Critere d'acceptation type recuit simule (CDC S8.6.2). Accepte
     * toujours une solution meilleure ou egale ; accepte une solution pire
     * avec une probabilite decroissante selon l'ecart de cout et la
     * temperature courante - permet d'echapper aux optima locaux ou une
     * construction gloutonne pure resterait definitivement bloquee.
     */
    private boolean accepterSelonRecuit(double coutCourant, double coutCandidat, double temperature) {
        if (coutCandidat <= coutCourant) {
            return true;
        }
        if (temperature <= 0.0001) {
            return false;
        }
        double probabilite = Math.exp((coutCourant - coutCandidat) / temperature);
        return random.nextDouble() < probabilite;
    }
}
