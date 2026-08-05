package com.fretcorridor.opt.algorithm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class KuhnMunkresSolverTest {

    @Test
    @DisplayName("Matrice 2x2 simple")
    void matrice2x2() {
        double[][] couts = {
            {1.0, 2.0},
            {2.0, 1.0}
        };
        int[] resultat = KuhnMunkresSolver.resoudre(couts);

        assertThat(resultat).hasSize(2);
        assertThat(resultat[0]).isEqualTo(0); // demande 0 -> capacite 0
        assertThat(resultat[1]).isEqualTo(1); // demande 1 -> capacite 1
    }

    @Test
    @DisplayName("Plus de demandes que de capacites")
    void plusDeDemandesQueDeCapacites() {
        double[][] couts = {
            {1.0},
            {2.0},
            {3.0}
        };
        int[] resultat = KuhnMunkresSolver.resoudre(couts);

        assertThat(resultat).hasSize(3);
        // La 3e demande doit avoir -1 (pas de capacite)
        long affectees = java.util.Arrays.stream(resultat).filter(i -> i >= 0).count();
        assertThat(affectees).isEqualTo(1);
    }

    @Test
    @DisplayName("Matrice carree 3x3")
    void matrice3x3() {
        double[][] couts = {
            {1.0, 2.0, 3.0},
            {3.0, 2.0, 1.0},
            {2.0, 1.0, 3.0}
        };
        int[] resultat = KuhnMunkresSolver.resoudre(couts);

        assertThat(resultat).hasSize(3);
        // Toutes les demandes doivent etre affectees
        for (int r : resultat) {
            assertThat(r).isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    @DisplayName("Matrice avec couts identiques")
    void coutsIdentiques() {
        double[][] couts = {
            {5.0, 5.0},
            {5.0, 5.0}
        };
        int[] resultat = KuhnMunkresSolver.resoudre(couts);

        assertThat(resultat).hasSize(2);
        assertThat(resultat[0]).isNotEqualTo(resultat[1]); // affectations differentes
    }

    @Test
    @DisplayName("Coûts avec zéros")
    void avecZeros() {
        double[][] couts = {
            {0.0, 1.0},
            {1.0, 0.0}
        };
        int[] resultat = KuhnMunkresSolver.resoudre(couts);

        assertThat(resultat).hasSize(2);
        assertThat(resultat[0]).isEqualTo(0);
        assertThat(resultat[1]).isEqualTo(1);
    }

    @Test
    @DisplayName("Matrice vide retourne tableau vide")
    void matriceVide() {
        double[][] couts = {};
        int[] resultat = KuhnMunkresSolver.resoudre(couts);

        assertThat(resultat).isEmpty();
    }

    @Test
    @DisplayName("Coût total minimal - verification")
    void coutTotalMinimal() {
        double[][] couts = {
            {10.0, 5.0, 8.0},
            {6.0, 9.0, 7.0},
            {4.0, 3.0, 2.0}
        };
        int[] r = KuhnMunkresSolver.resoudre(couts);

        double total = 0;
        for (int i = 0; i < r.length; i++) {
            if (r[i] >= 0) total += couts[i][r[i]];
        }
        // Solution optimale connue : 0->1(5) + 1->0(6) + 2->2(2) = 13
        assertThat(total).isEqualTo(13.0);
    }
}
