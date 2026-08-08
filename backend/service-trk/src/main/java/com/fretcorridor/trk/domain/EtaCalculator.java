package com.fretcorridor.trk.domain;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Calcul dynamique de l'ETA avec intervalle de confiance.
 *
 * Phase 1 (MVP) : version simplifiée sans post-traitement par apprentissage
 * (palier V3 du CDC §8.13). Utilise la vitesse moyenne observée sur les N
 * dernières positions pour projeter le temps restant jusqu'à la destination.
 *
 * RG-067 — Honnêteté de l'ETA : l'ETA est toujours présentée avec un
 * intervalle, jamais comme une valeur ponctuelle.
 *
 * RG-068 — Asymétrie du coût de l'erreur : la borne basse (optimiste) est
 * plus proche de l'estimation que la borne haute (pessimiste), car sous-estimer
 * coûte plus cher qu'annoncer trop tôt (CDC §8.11.3).
 */
@Component
public class EtaCalculator {

    /**
     * Nombre de positions récentes utilisées pour estimer la vitesse courante.
     * Configurable, mais valeur par défaut raisonnable pour un MVP sur un axe
     * interurbain où les positions arrivent toutes les ~30-60 secondes.
     */
    private static final int FENETRE_POSITIONS = 10;

    /**
     * Facteur d'élargissement de l'intervalle de confiance côté pessimiste.
     * 2.0 signifie que la borne haute est 2x plus éloignée de l'estimation
     * que la borne basse (asymétrie RG-068).
     */
    private static final double FACTEUR_ASYMMETRIE = 2.0;

    /**
     * Pourcentage d'élargissement de base de l'intervalle.
     * 0.30 = ±30% autour de l'estimation pour la borne basse,
     * ±60% (30% × 2.0) pour la borne haute.
     */
    private static final double ELARGISSEMENT_BASE = 0.30;

    /**
     * Vitesse minimale en km/h utilisée quand on n'a pas assez de données.
     * Évite une division par zéro et fournit une estimation conservatrice
     * (30 km/h sur réseau africain, où la moyenne est faible).
     */
    private static final double VITESSE_MINIMALE_KMH = 30.0;

    /**
     * Calcule l'ETA et son intervalle de confiance.
     *
     * @param positionsRécentes les N dernières positions connues (ordonnées par horodatageCapture croissant)
     * @param destinationLat    latitude de la destination
     * @param destinationLon    longitude de la destination
     * @return résultat contenant l'ETA estimée et les bornes de l'intervalle
     */
    public EtaResultat calculer(List<Position> positionsRécentes, double destinationLat, double destinationLon) {
        if (positionsRécentes == null || positionsRécentes.isEmpty()) {
            return EtaResultat.indisponible("Aucune position récente disponible");
        }

        // Dernière position connue
        Position dernière = positionsRécentes.get(positionsRécentes.size() - 1);

        // Distance restante jusqu'à destination (haversine)
        double distanceRestanteKm = haversine(
                dernière.getLatitude(), dernière.getLongitude(),
                destinationLat, destinationLon
        );

        // Vitesse moyenne estimée sur les positions récentes
        double vitesseKmh = estimerVitesse(positionsRécentes);

        // Temps restant estimé (heures)
        double tempsRestantHeures = distanceRestanteKm / vitesseKmh;

        // Conversion en Duration
        Duration tempsRestant = Duration.ofSeconds((long) (tempsRestantHeures * 3600));

        // Calcul de l'instant d'arrivée estimé
        Instant maintenant = Instant.now();
        Instant etaCentral = maintenant.plus(tempsRestant);

        // Intervalle de confiance asymétrique (RG-068)
        long secondesBorneBasse = (long) (tempsRestant.getSeconds() * (1 - ELARGISSEMENT_BASE));
        long secondesBorneHaute = (long) (tempsRestant.getSeconds() * (1 + ELARGISSEMENT_BASE * FACTEUR_ASYMMETRIE));

        // Les bornes ne peuvent pas être dans le passé
        Instant borneBasse = maintenant.plusSeconds(Math.max(0, secondesBorneBasse));
        Instant borneHaute = maintenant.plusSeconds(Math.max(0, secondesBorneHaute));

        return new EtaResultat(
                etaCentral,
                borneBasse,
                borneHaute,
                distanceRestanteKm,
                vitesseKmh,
                positionsRécentes.size(),
                dernière.getHorodatageCapture()
        );
    }

    /**
     * Estime la vitesse moyenne en km/h à partir des positions récentes.
     * Utilise la distance totale parcourue divisée par le temps écoulé
     * entre la première et la dernière position de la fenêtre.
     */
    private double estimerVitesse(List<Position> positions) {
        if (positions.size() < 2) {
            return VITESSE_MINIMALE_KMH;
        }

        // Prendre les FENETRE_POSITIONS dernières positions
        int debut = Math.max(0, positions.size() - FENETRE_POSITIONS);
        List<Position> fenetre = positions.subList(debut, positions.size());

        if (fenetre.size() < 2) {
            return VITESSE_MINIMALE_KMH;
        }

        Position première = fenetre.get(0);
        Position dernière = fenetre.get(fenetre.size() - 1);

        // Distance totale parcourue dans la fenêtre (somme des segments)
        double distanceTotaleKm = 0.0;
        for (int i = 1; i < fenetre.size(); i++) {
            Position p1 = fenetre.get(i - 1);
            Position p2 = fenetre.get(i);
            distanceTotaleKm += haversine(p1.getLatitude(), p1.getLongitude(),
                    p2.getLatitude(), p2.getLongitude());
        }

        // Temps écoulé entre première et dernière position (heures)
        double tempsEcouleHeures = Duration.between(
                première.getHorodatageCapture(),
                dernière.getHorodatageCapture()
        ).toSeconds() / 3600.0;

        if (tempsEcouleHeures <= 0) {
            return VITESSE_MINIMALE_KMH;
        }

        double vitesse = distanceTotaleKm / tempsEcouleHeures;

        // Plancher à la vitesse minimale
        return Math.max(vitesse, VITESSE_MINIMALE_KMH);
    }

    /**
     * Formule de Haversine — distance en km entre deux points GPS.
     * Implémentation directe, sans dépendance externe (pas de PostGIS
     * nécessaire pour ce calcul simple côté applicatif).
     */
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double RAYON_TERRE_KM = 6371.0;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return RAYON_TERRE_KM * c;
    }

    /**
     * Résultat du calcul d'ETA.
     */
    public record EtaResultat(
            Instant etaCentral,
            Instant borneBasse,
            Instant borneHaute,
            double distanceRestanteKm,
            double vitesseEstimeeKmh,
            int nombrePositionsUtilisees,
            Instant horodatageDernierePosition
    ) {
        /**
         * Crée un résultat indiquant l'indisponibilité du calcul.
         */
        public static EtaResultat indisponible(String raison) {
            return new EtaResultat(null, null, null, 0, 0, 0, null);
        }

        public boolean isDisponible() {
            return etaCentral != null;
        }
    }
}
