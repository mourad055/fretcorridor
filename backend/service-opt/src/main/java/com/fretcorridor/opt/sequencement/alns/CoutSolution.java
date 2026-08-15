package com.fretcorridor.opt.sequencement.alns;

import java.util.Map;

/**
 * RG-107 (CDC S8.6.3) : "il n'existe pas de 'trajet le plus optimise' dans
 * l'absolu ; il existe un trajet optimal pour une ponderation donnee, et
 * cette ponderation est une decision de gestion, pas une propriete
 * mathematique." Coefficients lus depuis Axe.parametres (EF-GEO-02), meme
 * pattern que rayonAppariementKm/detourMaxDistanceKm - jamais code en dur.
 *
 * TODO (bloque en amont, pas oublie ici) : le terme "retard" reste a 0 tant
 * que DemandePubliee ne porte pas de fenetre temporelle (fenetreDebut/
 * fenetreFin) - absente du contrat actuel (shared-contracts/asyncapi/
 * events/demande-publiee.yaml). A activer des que Mobile publie ce champ,
 * sans autre changement de code ici (le coefficient est deja lu depuis la
 * config, juste jamais alimente par une penalite non nulle pour l'instant).
 */
public class CoutSolution {

    public record Poids(double distanceKm, double nbVehicules, double retard, double remplissage) {
        public static Poids depuis(Map<String, Object> parametresAxe) {
            return new Poids(
                    extraire(parametresAxe, "pondCoutDistance", 1.0),
                    extraire(parametresAxe, "pondCoutVehicules", 0.0),
                    extraire(parametresAxe, "pondCoutRetard", 0.0),
                    extraire(parametresAxe, "pondCoutRemplissage", 0.0)
            );
        }

        private static double extraire(Map<String, Object> parametres, String cle, double defaut) {
            if (parametres == null) return defaut;
            Object v = parametres.get(cle);
            return (v instanceof Number n) ? n.doubleValue() : defaut;
        }
    }

    private final Poids poids;

    public CoutSolution(Poids poids) {
        this.poids = poids;
    }

    /**
     * Score composite : plus bas = meilleure solution (coherent avec la
     * convention deja utilisee cote MAT, CoutCompositeService). Le terme
     * "retard" est actuellement toujours 0 (cf TODO en tete de classe).
     */
    public double evaluer(double distanceTotaleKm, int nbVehiculesUtilises,
                           double retardTotalMinutes, double tauxRemplissageMoyen) {
        return poids.distanceKm() * distanceTotaleKm
                + poids.nbVehicules() * nbVehiculesUtilises
                + poids.retard() * retardTotalMinutes
                // remplissage : objectif a MAXIMISER, donc penalise son complement
                // (1 - taux) plutot que de le soustraire directement - garde le
                // score globalement homogene en "plus bas = mieux".
                + poids.remplissage() * (1.0 - tauxRemplissageMoyen);
    }
}
