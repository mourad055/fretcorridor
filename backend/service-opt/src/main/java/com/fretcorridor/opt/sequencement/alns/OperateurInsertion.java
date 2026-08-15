package com.fretcorridor.opt.sequencement.alns;

import com.fretcorridor.dto.PointGeoDto;
import com.fretcorridor.opt.domain.Affectation;
import com.fretcorridor.util.HaversineUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Operateur de reconstruction ALNS (CDC S8.6.2) : insertion "moins cher
 * realisable" (cheapest feasible insertion) - pour chaque paire de positions
 * (rangEnlevement, rangLivraison) possible, verifie TOUTES les contraintes
 * dures (precedence dejà garantie par construction, capacite dynamique via
 * EtatSolution.avecInsertion, detour EF-MAT-10 via DetourValidator), retient
 * la position qui minimise le cout marginal parmi celles qui restent
 * realisables. Aucune position realisable = l'affectation reste non
 * inseree ce cycle (jamais forcee dans une solution infaisable).
 */
public class OperateurInsertion {

    private static final Logger log = LoggerFactory.getLogger(OperateurInsertion.class);

    private final DetourValidator detourValidator;

    public OperateurInsertion(DetourValidator detourValidator) {
        this.detourValidator = detourValidator;
    }

    public record ResultatInsertion(EtatSolution solution, double coutMarginalKm) {
    }

    /**
     * @param affectation         demande a inserer
     * @param solutionCourante    sequence avant insertion
     * @param positionsAffectations  map affectationId -> (origine, destination) de
     *        chaque affectation deja dans la sequence, pour reconstruire le
     *        trajet reel et calculer le detour de la demande evaluee
     * @param parametresAxe       Axe.parametres (detourMaxDistanceKm, cf DetourValidator)
     */
    public ResultatInsertion trouverMeilleurePosition(Affectation affectation,
                                                        EtatSolution solutionCourante,
                                                        Map<UUID, PointGeoDto[]> positionsAffectations,
                                                        Map<String, Object> parametresAxe) {
        int tailleActuelle = solutionCourante.taille();
        ResultatInsertion meilleur = null;

        PointGeoDto origineDemande = new PointGeoDto(affectation.getOrigineLatitude(), affectation.getOrigineLongitude());
        PointGeoDto destinationDemande = new PointGeoDto(affectation.getDestinationLatitude(), affectation.getDestinationLongitude());

        // Parcourt toutes les paires de positions valides (enlevement avant
        // livraison, cf EtatSolution.avecInsertion qui rejette sinon) - O(n^2)
        // sur la taille de la tournee, acceptable pour les tailles visees en V1
        // (consolidation de quelques demandes, pas des centaines).
        for (int rangEnlevement = 0; rangEnlevement <= tailleActuelle; rangEnlevement++) {
            for (int rangLivraison = rangEnlevement; rangLivraison <= tailleActuelle; rangLivraison++) {

                EtatSolution candidate = solutionCourante.avecInsertion(affectation, rangEnlevement, rangLivraison);
                if (candidate == null) {
                    continue; // capacite dynamique violee a cette position - rejete
                }

                double distanceReelleKm = calculerDistanceParcourueParDemande(
                        candidate, affectation.getId(), positionsAffectations, origineDemande, destinationDemande);

                boolean detourOk = detourValidator.detourAcceptable(
                        origineDemande, destinationDemande, distanceReelleKm, parametresAxe);
                if (!detourOk) {
                    continue; // EF-MAT-10 : contrainte dure, position rejetee
                }

                double coutMarginal = distanceReelleKm; // V1 : cout marginal = distance ajoutee
                if (meilleur == null || coutMarginal < meilleur.coutMarginalKm()) {
                    meilleur = new ResultatInsertion(candidate, coutMarginal);
                }
            }
        }

        if (meilleur == null) {
            log.debug("Aucune position realisable pour l'affectation {} (capacite ou detour) - "
                    + "reste non inseree ce cycle.", affectation.getId());
        }
        return meilleur;
    }

    /**
     * Distance Haversine reellement parcourue par la demande donnee entre son
     * enlevement et sa livraison dans la sequence candidate, en sommant les
     * segments intermediaires (autres arrets consolides sur le trajet).
     */
    private double calculerDistanceParcourueParDemande(EtatSolution solution, UUID affectationId,
                                                         Map<UUID, PointGeoDto[]> positionsAffectations,
                                                         PointGeoDto origineDemande, PointGeoDto destinationDemande) {
        List<EtatSolution.PositionPlanifiee> sequence = solution.getSequence();

        int indexEnlevement = -1, indexLivraison = -1;
        for (int i = 0; i < sequence.size(); i++) {
            EtatSolution.PositionPlanifiee p = sequence.get(i);
            if (p.affectationId().equals(affectationId) && p.type() == EtatSolution.TypeArret.ENLEVEMENT) indexEnlevement = i;
            if (p.affectationId().equals(affectationId) && p.type() == EtatSolution.TypeArret.LIVRAISON) indexLivraison = i;
        }

        double distanceKm = 0.0;
        PointGeoDto pointPrecedent = origineDemande;
        for (int i = indexEnlevement; i <= indexLivraison; i++) {
            PointGeoDto pointCourant = resoudrePoint(sequence.get(i), positionsAffectations, origineDemande, destinationDemande);
            distanceKm += HaversineUtils.distance(pointPrecedent, pointCourant);
            pointPrecedent = pointCourant;
        }
        return distanceKm;
    }

    private PointGeoDto resoudrePoint(EtatSolution.PositionPlanifiee position,
                                       Map<UUID, PointGeoDto[]> positionsAffectations,
                                       PointGeoDto origineDemandeEvaluee, PointGeoDto destinationDemandeEvaluee) {
        PointGeoDto[] paire = positionsAffectations.get(position.affectationId());
        if (paire == null) {
            // L'affectation evaluee elle-meme n'est pas encore dans la map appelante.
            return position.type() == EtatSolution.TypeArret.ENLEVEMENT ? origineDemandeEvaluee : destinationDemandeEvaluee;
        }
        return position.type() == EtatSolution.TypeArret.ENLEVEMENT ? paire[0] : paire[1];
    }
}
