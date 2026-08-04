package com.fretcorridor.opt.algorithm;

import java.util.Arrays;

/**
 * Algorithme hongrois (Kuhn-Munkres) pour l'affectation optimale a cout
 * minimal entre deux ensembles (ici : demandes x capacites), sur une matrice
 * de cout rectangulaire.
 *
 * Choix explicite plutot qu'un appariement "au plus proche disponible" :
 * anti-patron explicite du CDC (EF-MAT-01, "jamais glouton"). Complexite
 * O(n^3) sur le plus grand des deux dimensions - largement suffisant pour le
 * budget de latence L0/L1 ~50ms sur les tailles de lot attendues (le
 * filtrage H3 en amont reduit deja fortement l'espace de recherche).
 *
 * Matrice rectangulaire geree par padding : si le nombre de demandes et de
 * capacites differe, on complete avec un cout "sentinelle" tres eleve plutot
 * qu'un zero artificiel - un zero serait pris pour un candidat ideal par
 * l'algorithme, ce qui fausserait l'affectation reelle. Les paires impliquant
 * un indice de padding sont filtrees en sortie (valeur -1).
 */
public final class KuhnMunkresSolver {

    // Superieur a toute somme ponderee plausible (les couts MAT sont bornes
    // dans [0,1] par candidat/critere, cf validation CoutController cote
    // service-mat) - jamais choisi tant qu'une paire reelle reste disponible.
    private static final double COUT_SENTINELLE = 1_000_000.0;

    private KuhnMunkresSolver() {
        // Utilitaire statique, pas d'instanciation.
    }

    /**
     * Resout l'affectation a cout minimal sur une matrice rectangulaire
     * couts[demande][capacite].
     *
     * @param couts matrice de cout, couts[i][j] = cout de la paire (demande i,
     *              capacite j). Doit etre rectangulaire et non vide.
     * @return pour chaque ligne i (indice original), l'indice de colonne j
     *         affectee, ou -1 si aucune capacite reelle n'a pu etre affectee
     *         a cette demande (plus de demandes que de capacites dans le lot).
     */
    public static int[] resoudre(double[][] couts) {
        if (couts == null || couts.length == 0 || couts[0].length == 0) {
            throw new IllegalArgumentException("Matrice de couts vide ou nulle");
        }

        int nbDemandes = couts.length;
        int nbCapacites = couts[0].length;
        for (double[] ligne : couts) {
            if (ligne.length != nbCapacites) {
                throw new IllegalArgumentException("Matrice de couts non rectangulaire");
            }
        }

        // Padding en carre n x n (n = max(nbDemandes, nbCapacites)), 1-indexe
        // (ligne/colonne 0 inutilisee) - convention de l'algorithme classique.
        int n = Math.max(nbDemandes, nbCapacites);
        double[][] matriceCarree = new double[n + 1][n + 1];
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= n; j++) {
                matriceCarree[i][j] = (i <= nbDemandes && j <= nbCapacites)
                        ? couts[i - 1][j - 1]
                        : COUT_SENTINELLE;
            }
        }

        // --- Algorithme hongrois par potentiels (u, v) et chemins augmentants ---
        double[] u = new double[n + 1];
        double[] v = new double[n + 1];
        int[] affectationColonne = new int[n + 1]; // ligne affectee a la colonne j (0 = aucune)
        int[] parent = new int[n + 1];

        for (int i = 1; i <= n; i++) {
            affectationColonne[0] = i;
            int colonneCourante = 0;
            double[] minDelta = new double[n + 1];
            boolean[] visitee = new boolean[n + 1];
            Arrays.fill(minDelta, Double.MAX_VALUE);

            do {
                visitee[colonneCourante] = true;
                int ligne = affectationColonne[colonneCourante];
                double delta = Double.MAX_VALUE;
                int colonneSuivante = -1;

                for (int j = 1; j <= n; j++) {
                    if (!visitee[j]) {
                        double cout = matriceCarree[ligne][j] - u[ligne] - v[j];
                        if (cout < minDelta[j]) {
                            minDelta[j] = cout;
                            parent[j] = colonneCourante;
                        }
                        if (minDelta[j] < delta) {
                            delta = minDelta[j];
                            colonneSuivante = j;
                        }
                    }
                }

                for (int j = 0; j <= n; j++) {
                    if (visitee[j]) {
                        u[affectationColonne[j]] += delta;
                        v[j] -= delta;
                    } else {
                        minDelta[j] -= delta;
                    }
                }

                colonneCourante = colonneSuivante;
            } while (affectationColonne[colonneCourante] != 0);

            while (colonneCourante != 0) {
                int colonneParente = parent[colonneCourante];
                affectationColonne[colonneCourante] = affectationColonne[colonneParente];
                colonneCourante = colonneParente;
            }
        }

        int[] resultat = new int[nbDemandes];
        Arrays.fill(resultat, -1);
        for (int j = 1; j <= n; j++) {
            int ligne = affectationColonne[j];
            if (ligne >= 1 && ligne <= nbDemandes && j <= nbCapacites) {
                resultat[ligne - 1] = j - 1;
            }
        }

        return resultat;
    }
}
