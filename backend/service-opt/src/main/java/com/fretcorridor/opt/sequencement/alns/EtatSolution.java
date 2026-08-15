package com.fretcorridor.opt.sequencement.alns;

import com.fretcorridor.opt.domain.Affectation;
import com.fretcorridor.opt.sequencement.EtapeTournee;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Solution candidate en cours d'exploration ALNS (CDC S8.6.2) - PAS l'entite
 * JPA Tournee/EtapeTournee, qui ne sont persistees qu'une fois la recherche
 * terminee (cf AlnsSolver). Separation deliberee : construire/detruire des
 * milliers de solutions candidates ne doit jamais toucher la base.
 *
 * Une PositionPlanifiee = un enlevement ou une livraison d'une Affectation,
 * a une position (rang) donnee dans la sequence.
 */
public class EtatSolution {

    public enum TypeArret { ENLEVEMENT, LIVRAISON }

    public record PositionPlanifiee(UUID affectationId, TypeArret type, BigDecimal chargeApres) {
    }

    private final List<PositionPlanifiee> sequence;
    private final BigDecimal capaciteMaxKg;

    public EtatSolution(BigDecimal capaciteMaxKg) {
        this.sequence = new ArrayList<>();
        this.capaciteMaxKg = capaciteMaxKg;
    }

    private EtatSolution(List<PositionPlanifiee> sequence, BigDecimal capaciteMaxKg) {
        this.sequence = new ArrayList<>(sequence);
        this.capaciteMaxKg = capaciteMaxKg;
    }

    public EtatSolution copie() {
        return new EtatSolution(this.sequence, this.capaciteMaxKg);
    }

    public List<PositionPlanifiee> getSequence() {
        return List.copyOf(sequence);
    }

    /**
     * Insere l'enlevement ET la livraison d'une affectation, aux positions
     * donnees (rangEnlevement < rangLivraison verifie par l'appelant -
     * precedence, CDC S8.6.1 point 2). Recalcule la charge dynamique de
     * TOUTE la sequence apres insertion - simulation complete, jamais une
     * somme globale (CDC S8.6.1 point 3, piege explicite documente).
     *
     * @return null si l'insertion viole la capacite dynamique a un
     *         quelconque point intermediaire de la sequence resultante -
     *         l'appelant (OperateurInsertion) doit essayer une autre
     *         position plutot que forcer une solution infaisable.
     */
    public EtatSolution avecInsertion(Affectation affectation, int rangEnlevement, int rangLivraison) {
        if (rangEnlevement >= rangLivraison) {
            throw new IllegalArgumentException(
                    "Precedence violee : enlevement (" + rangEnlevement + ") doit precede livraison ("
                            + rangLivraison + ")");
        }

        List<PositionPlanifiee> nouvelle = new ArrayList<>(sequence);
        BigDecimal poids = affectation.getPoidsTaxableKg() != null
                ? affectation.getPoidsTaxableKg() : BigDecimal.ZERO;

        nouvelle.add(rangEnlevement, new PositionPlanifiee(affectation.getId(), TypeArret.ENLEVEMENT, null));
        // rangLivraison decale de 1 par l'insertion precedente si elle etait apres.
        int rangLivraisonAjuste = rangLivraison >= rangEnlevement ? rangLivraison + 1 : rangLivraison;
        nouvelle.add(rangLivraisonAjuste, new PositionPlanifiee(affectation.getId(), TypeArret.LIVRAISON, null));

        return recalculerChargeOuNull(nouvelle, poids);
    }

    /**
     * Retourne une nouvelle sequence avec la charge dynamique recalculee a
     * chaque position, ou null si la capacite est depassee a un point
     * quelconque - jamais une solution incoherente retournee silencieusement.
     */
    private EtatSolution recalculerChargeOuNull(List<PositionPlanifiee> brut, BigDecimal poidsNouvelleAffectation) {
        List<PositionPlanifiee> recalculee = new ArrayList<>(brut.size());
        BigDecimal chargeCourante = BigDecimal.ZERO;

        // Table des poids par affectation deja connue dans la sequence courante
        // (necessaire pour recalculer les LIVRAISON existantes correctement).
        java.util.Map<UUID, BigDecimal> poidsParAffectation = new java.util.HashMap<>();
        for (PositionPlanifiee p : brut) {
            poidsParAffectation.putIfAbsent(p.affectationId(), poidsNouvelleAffectation);
        }

        for (PositionPlanifiee p : brut) {
            BigDecimal poidsAffectation = poidsParAffectation.getOrDefault(p.affectationId(), BigDecimal.ZERO);
            chargeCourante = p.type() == TypeArret.ENLEVEMENT
                    ? chargeCourante.add(poidsAffectation)
                    : chargeCourante.subtract(poidsAffectation);

            if (capaciteMaxKg != null && chargeCourante.compareTo(capaciteMaxKg) > 0) {
                return null; // capacite depassee a CET etat intermediaire precis
            }
            if (chargeCourante.compareTo(BigDecimal.ZERO) < 0) {
                return null; // livraison sans enlevement correspondant - sequence incoherente
            }

            recalculee.add(new PositionPlanifiee(p.affectationId(), p.type(), chargeCourante));
        }

        return new EtatSolution(recalculee, capaciteMaxKg);
    }

    public int taille() {
        return sequence.size();
    }

    public boolean estVide() {
        return sequence.isEmpty();
    }
}
