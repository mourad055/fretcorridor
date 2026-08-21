package com.fretcorridor.opt.sequencement.alns;

import com.fretcorridor.opt.domain.Affectation;

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
 * a une position (rang) donnee dans la sequence. Porte son propre poids
 * (correctif Sprint 12) : l'ancienne version recalculait le poids de TOUTE
 * la sequence a partir du poids de la SEULE affectation nouvellement inseree
 * (poidsParAffectation.putIfAbsent applique a tort a tous les IDs distincts) -
 * faux des qu'une tournee contient 2 affectations de poids differents. En
 * stockant le poids directement sur chaque position, plus besoin de le
 * reconstruire : chaque position connait son propre poids, point final.
 */
public class EtatSolution {

    public enum TypeArret { ENLEVEMENT, LIVRAISON }

    public record PositionPlanifiee(UUID affectationId, TypeArret type, BigDecimal poids, BigDecimal chargeApres) {
    }

    private final List<PositionPlanifiee> sequence;
    private final BigDecimal capaciteMaxKg;
    private final BigDecimal chargeInitiale;

    public EtatSolution(BigDecimal capaciteMaxKg) {
        this(capaciteMaxKg, BigDecimal.ZERO);
    }

    /**
     * chargeInitiale (Sprint 12, EF-MAT-09) : poids deja a bord du vehicule
     * au moment ou cette recherche demarre - cas d'une replanification en
     * cours de tournee ou une marchandise a ete enlevee (etape EXECUTEE)
     * mais pas encore livree. BigDecimal.ZERO pour une construction initiale
     * (vehicule vide au depart, cf SequencementDeclencheur). Jamais null en
     * interne (normalise ici une bonne fois, pas a chaque appelant).
     */
    public EtatSolution(BigDecimal capaciteMaxKg, BigDecimal chargeInitiale) {
        this.sequence = new ArrayList<>();
        this.capaciteMaxKg = capaciteMaxKg;
        this.chargeInitiale = chargeInitiale != null ? chargeInitiale : BigDecimal.ZERO;
    }

    private EtatSolution(List<PositionPlanifiee> sequence, BigDecimal capaciteMaxKg, BigDecimal chargeInitiale) {
        this.sequence = new ArrayList<>(sequence);
        this.capaciteMaxKg = capaciteMaxKg;
        this.chargeInitiale = chargeInitiale;
    }

    public EtatSolution copie() {
        return new EtatSolution(this.sequence, this.capaciteMaxKg, this.chargeInitiale);
    }

    public List<PositionPlanifiee> getSequence() {
        return List.copyOf(sequence);
    }

    /**
     * Insere l'enlevement ET la livraison d'une affectation, aux positions
     * donnees (rangEnlevement <= rangLivraison verifie ci-dessous -
     * precedence, CDC S8.6.1 point 2). Recalcule la charge dynamique de
     * TOUTE la sequence apres insertion, a partir de chargeInitiale -
     * simulation complete, jamais une somme globale (CDC S8.6.1 point 3).
     *
     * @return null si l'insertion viole la capacite dynamique a un
     *         quelconque point intermediaire de la sequence resultante.
     */
    public EtatSolution avecInsertion(Affectation affectation, int rangEnlevement, int rangLivraison) {
        if (rangEnlevement > rangLivraison) {
            throw new IllegalArgumentException(
                    "Precedence violee : enlevement (" + rangEnlevement + ") doit precede livraison ("
                            + rangLivraison + ")");
        }

        List<PositionPlanifiee> nouvelle = new ArrayList<>(sequence);
        BigDecimal poids = affectation.getPoidsTaxableKg() != null
                ? affectation.getPoidsTaxableKg() : BigDecimal.ZERO;

        nouvelle.add(rangEnlevement, new PositionPlanifiee(affectation.getId(), TypeArret.ENLEVEMENT, poids, null));
        int rangLivraisonAjuste = rangLivraison >= rangEnlevement ? rangLivraison + 1 : rangLivraison;
        nouvelle.add(rangLivraisonAjuste, new PositionPlanifiee(affectation.getId(), TypeArret.LIVRAISON, poids, null));

        return recalculerChargeOuNull(nouvelle);
    }

    /**
     * Insere UNIQUEMENT une livraison, sans enlevement associe - cas d'une
     * affectation dont l'enlevement est deja EXECUTEE (marchandise a bord,
     * comptee dans chargeInitiale) lors d'une replanification (Sprint 12,
     * ReplanificationService). Jamais utilise pour une affectation neuve.
     */
    public EtatSolution avecInsertionLivraisonSeule(UUID affectationId, BigDecimal poids, int rangLivraison) {
        List<PositionPlanifiee> nouvelle = new ArrayList<>(sequence);
        nouvelle.add(rangLivraison, new PositionPlanifiee(affectationId, TypeArret.LIVRAISON, poids, null));
        return recalculerChargeOuNull(nouvelle);
    }

    /**
     * Retire de la sequence toutes les positions (enlevement + livraison)
     * des affectations dont l'id figure dans affectationIds - operateur de
     * destruction ALNS (CDC S8.6.2, cf OperateurRetrait). Un retrait pur ne
     * peut jamais violer la capacite (elle ne fait que baisser a chaque
     * etape) ni produire une charge negative si la sequence d'origine etait
     * valide - le resultat n'est donc jamais null par construction, mais on
     * repasse par recalculerChargeOuNull() pour re-synchroniser chargeApres
     * sur chaque position restante (impactee par le retrait).
     */
    public EtatSolution sansAffectations(java.util.Set<UUID> affectationIds) {
        List<PositionPlanifiee> filtree = sequence.stream()
                .filter(p -> !affectationIds.contains(p.affectationId()))
                .toList();
        return recalculerChargeOuNull(filtree);
    }

    /**
     * Recalcule la charge dynamique de TOUTE la sequence a partir de
     * chargeInitiale (jamais zero fixe) - chaque position porte deja son
     * propre poids, plus de reconstruction approximative par affectationId
     * (bug corrige Sprint 12, cf javadoc de classe).
     */
    private EtatSolution recalculerChargeOuNull(List<PositionPlanifiee> brut) {
        List<PositionPlanifiee> recalculee = new ArrayList<>(brut.size());
        BigDecimal chargeCourante = chargeInitiale;

        for (PositionPlanifiee p : brut) {
            BigDecimal poidsAffectation = p.poids() != null ? p.poids() : BigDecimal.ZERO;
            chargeCourante = p.type() == TypeArret.ENLEVEMENT
                    ? chargeCourante.add(poidsAffectation)
                    : chargeCourante.subtract(poidsAffectation);

            if (capaciteMaxKg != null && chargeCourante.compareTo(capaciteMaxKg) > 0) {
                return null; // capacite depassee a CET etat intermediaire precis
            }
            if (chargeCourante.compareTo(BigDecimal.ZERO) < 0) {
                return null; // livraison sans enlevement correspondant - sequence incoherente
            }

            recalculee.add(new PositionPlanifiee(p.affectationId(), p.type(), p.poids(), chargeCourante));
        }

        return new EtatSolution(recalculee, capaciteMaxKg, chargeInitiale);
    }

    public int taille() {
        return sequence.size();
    }

    public boolean estVide() {
        return sequence.isEmpty();
    }
}
