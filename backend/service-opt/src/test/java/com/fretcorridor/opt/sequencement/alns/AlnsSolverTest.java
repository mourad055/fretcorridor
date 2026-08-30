package com.fretcorridor.opt.sequencement.alns;

import com.fretcorridor.opt.domain.Affectation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de AlnsSolver (audit CDC suivi 2026-08-20, constat "AlnsSolver pas
 * un vrai ALNS" - constat obsolete depuis 614a482, mais AUCUN test
 * n'existait pour la boucle destroy/repair introduite a ce commit. Cette
 * classe comble ce trou : verifie en execution reelle (pas en lecture de
 * code) les proprietes que la javadoc de AlnsSolver revendique.
 *
 * Ne teste PAS l'exactitude de l'optimum (le recuit simule est stochastique,
 * pas de garantie d'optimalite) - teste les invariants structurels qui
 * DOIVENT tenir quel que soit le tirage aleatoire : completude, respect de
 * la capacite, robustesse au cas vide, non-degradation par l'elitisme.
 */
class AlnsSolverTest {

    private final DetourValidator detourValidator = new DetourValidator();
    private final OperateurInsertion operateurInsertion = new OperateurInsertion(detourValidator);
    // Seed fixe (fix audit 21/08, reproductibilite EF-MAT-11/12) : les
    // invariants testes ici doivent tenir quel que soit le tirage - un seed
    // deterministe rend en plus chaque run de test reproductible.
    private final AlnsSolver solver = new AlnsSolver(operateurInsertion, 42L);

    /**
     * Affectation.id est @GeneratedValue (JPA) - null hors contexte de
     * persistance. AlnsSolver indexe par Affectation::getId
     * (Collectors.toMap) : sans id distinct, toutes les instances de test
     * collisionneraient sur une cle null. Fixe l'id par reflexion, seul
     * moyen sans ouvrir un EntityManager pour un test unitaire pur.
     */
    private Affectation creerAffectation(double poidsKg, double origineLat, double origineLon,
                                          double destLat, double destLon) {
        Affectation affectation = new Affectation(
                UUID.randomUUID(), UUID.randomUUID(), null, null, null,
                BigDecimal.valueOf(poidsKg),
                origineLat, origineLon, destLat, destLon,
                null, null, null, null,
                BigDecimal.TEN,
                null, null, null,
                null, null, null, null,
                null, null,
                null, null, null,
                false,
                null, null, null, null, null, null, null, null, null
        );
        try {
            Field idField = Affectation.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(affectation, UUID.randomUUID());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Impossible de fixer l'id de test par reflexion", e);
        }
        return affectation;
    }

    @Test
    void listeVide_neLanceAucuneException_etRetourneUnResultatVide() {
        AlnsSolver.ResultatSequencement resultat = solver.resoudre(
                List.of(), BigDecimal.valueOf(1000), BigDecimal.ZERO, Map.of());

        assertTrue(resultat.affectationsInserees().isEmpty());
        assertTrue(resultat.affectationsNonInserees().isEmpty());
        assertTrue(resultat.solutionFinale().estVide());
    }

    @Test
    void casFaisable_toutesLesAffectationsSontInserees() {
        // Deux affectations proches geographiquement, poids largement sous
        // la capacite, aucune borne de detour (Map.of() = permissif, cf
        // DetourValidator.extraireDetourMaxKm) - rien ne doit empecher
        // l'insertion des deux, quel que soit le tirage aleatoire du recuit.
        Affectation a1 = creerAffectation(300, 4.05, 9.70, 4.06, 9.72);
        Affectation a2 = creerAffectation(300, 4.06, 9.71, 4.07, 9.73);

        AlnsSolver.ResultatSequencement resultat = solver.resoudre(
                List.of(a1, a2), BigDecimal.valueOf(1000), BigDecimal.ZERO, Map.of());

        assertEquals(2, resultat.affectationsInserees().size(),
                "Cas trivialement faisable : les deux affectations doivent etre inserees");
        assertTrue(resultat.affectationsNonInserees().isEmpty());
    }

    @Test
    void completude_inserees_plus_nonInserees_egale_total_entree() {
        // Invariant structurel qui doit tenir independamment de la
        // faisabilite ou du tirage aleatoire : aucune affectation ne doit
        // disparaitre ou etre dupliquee entre construction initiale et
        // boucle destroy/repair.
        Affectation a1 = creerAffectation(200, 4.05, 9.70, 4.06, 9.72);
        Affectation a2 = creerAffectation(200, 4.06, 9.71, 4.07, 9.73);
        Affectation a3 = creerAffectation(200, 4.07, 9.72, 4.08, 9.74);
        Affectation a4 = creerAffectation(200, 4.08, 9.73, 4.09, 9.75);

        AlnsSolver.ResultatSequencement resultat = solver.resoudre(
                List.of(a1, a2, a3, a4), BigDecimal.valueOf(1000), BigDecimal.ZERO, Map.of());

        int total = resultat.affectationsInserees().size() + resultat.affectationsNonInserees().size();
        assertEquals(4, total);
    }

    @Test
    void capaciteDepassee_auMoinsUneAffectationResteNonInseree() {
        // FIX audit 21/08 : la version precedente de ce test etait fausse -
        // elle supposait une capacite STATIQUE (somme des poids <= capacite),
        // alors que EtatSolution implemente la capacite DYNAMIQUE du CDC
        // (S8.6.1 point 3 : "simulation complete, jamais une somme globale").
        // Deux affectations de 400 kg sur un vehicule de 500 kg sont
        // parfaitement consolidables SEQUENTIELLEMENT (livraison de la
        // premiere avant enlevement de la seconde : charge max 400, jamais
        // 800) - le solveur avait raison d'inserer les deux, le test echouait
        // de maniere deterministe des son commit.
        //
        // Pour tester reellement le rejet capacite, on rend le depassement
        // inevitable a TOUTE position : charge initiale 300 kg (marchandise
        // deja a bord, Sprint 12) + affectation de 400 kg sur capacite 500 -
        // 300+400=700 > 500 ou que l'on insere, y compris seule dans la
        // sequence.
        Affectation a1 = creerAffectation(400, 4.05, 9.70, 4.06, 9.72);

        AlnsSolver.ResultatSequencement resultat = solver.resoudre(
                List.of(a1), BigDecimal.valueOf(500), BigDecimal.valueOf(300), Map.of());

        assertFalse(resultat.affectationsNonInserees().isEmpty(),
                "Charge initiale 300kg + affectation 400kg sur capacite 500kg : "
                        + "l'affectation doit rester non inseree");

        // Verifie aussi que la solution retournee ne viole jamais la
        // capacite a aucun etat intermediaire (double controle : la
        // construction interne le garantit deja via recalculerChargeOuNull,
        // ce test verifie que rien n'a contourne cette garde).
        BigDecimal chargeMax = resultat.solutionFinale().getSequence().stream()
                .map(EtatSolution.PositionPlanifiee::chargeApres)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        assertTrue(chargeMax.compareTo(BigDecimal.valueOf(500)) <= 0,
                "La charge ne doit jamais depasser la capacite a aucun point de la sequence");
    }

    @Test
    void consolidationSequentielle_deuxPoidsSuperieursALaCapaciteStatique_sontInsertables() {
        // Complement du fix ci-dessus : verifie le comportement CORRECT du
        // solveur sur le cas qui cassait l'ancien test - deux affectations
        // de 400 kg sur capacite 500 kg doivent etre consolidees (l'une
        // livree avant que l'autre soit enlevee), c'est precisement ce qui
        // fait exister le groupage (CDC S8.5.3, gain de remplissage).
        Affectation a1 = creerAffectation(400, 4.05, 9.70, 4.06, 9.72);
        Affectation a2 = creerAffectation(400, 4.06, 9.71, 4.07, 9.73);

        AlnsSolver.ResultatSequencement resultat = solver.resoudre(
                List.of(a1, a2), BigDecimal.valueOf(500), BigDecimal.ZERO, Map.of());

        assertEquals(2, resultat.affectationsInserees().size(),
                "Consolidation sequentielle : les deux affectations doivent etre inserees");
        assertTrue(resultat.affectationsNonInserees().isEmpty());

        BigDecimal chargeMax = resultat.solutionFinale().getSequence().stream()
                .map(EtatSolution.PositionPlanifiee::chargeApres)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        assertTrue(chargeMax.compareTo(BigDecimal.valueOf(500)) <= 0,
                "La charge dynamique ne doit jamais depasser la capacite");
    }

    @Test
    void elitisme_laMeilleureSolutionEstAuMoinsAussiBonneQueLaConstructionInitiale() {
        // La javadoc de AlnsSolver revendique l'elitisme : "la solution
        // retournee est la MEILLEURE trouvee sur l'ensemble des iterations,
        // jamais simplement l'etat courant final". Verification indirecte :
        // sur un cas ou la construction gloutonne seule inserait deja tout,
        // le destroy/repair ne doit jamais faire REGRESSER le nombre
        // d'affectations inserees en dessous de ce que le glouton seul
        // obtenait.
        Affectation a1 = creerAffectation(100, 4.05, 9.70, 4.06, 9.72);
        Affectation a2 = creerAffectation(100, 4.06, 9.71, 4.07, 9.73);
        Affectation a3 = creerAffectation(100, 4.07, 9.72, 4.08, 9.74);

        AlnsSolver.ResultatSequencement resultat = solver.resoudre(
                List.of(a1, a2, a3), BigDecimal.valueOf(1000), BigDecimal.ZERO, Map.of());

        assertEquals(3, resultat.affectationsInserees().size(),
                "L'elitisme ne doit jamais retourner un resultat pire que la construction initiale gloutonne");
    }
}
