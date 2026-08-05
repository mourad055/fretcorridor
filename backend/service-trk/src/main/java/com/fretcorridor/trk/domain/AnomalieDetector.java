package com.fretcorridor.trk.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Détection d'anomalies de suivi GPS.
 *
 * EF-TRK-03 : Détecte les anomalies suivantes :
 * - Arrêt prolongé (immobilisation au-delà d'un seuil)
 * - Écart d'itinéraire (distance au corridor attendu)
 * - Absence prolongée de position (silence radio)
 *
 * EF-TRK-04 : Toute anomalie détectée porte l'âge de la dernière position
 * et la source de capture.
 *
 * Phase 1 (MVP) : version simplifiée sans référence à l'itinéraire OPT
 * (la dépendance GEO fournit les hubs de l'axe pour détecter un écart
 * grossier, pas un écart fin à l'itinéraire planifié).
 */
@Component
public class AnomalieDetector {

    private static final Logger log = LoggerFactory.getLogger(AnomalieDetector.class);

    /**
     * Seuil d'arrêt prolongé : si le véhicule parcourt moins que cette
     * distance (en km) pendant la durée du seuil, on considère qu'il est
     * à l'arrêt. Valeur conservative : un véhicule qui se gare, décharge
     * partiellement, ou attend à un checkpoint peut rester immobile 30 min.
     */
    private static final double SEUIL_DISTANCE_ARRET_KM = 0.1;

    /**
     * Durée au-delà de laquelle une immobilité est considérée comme anormale.
     */
    private static final Duration SEUIL_DUREE_ARRET = Duration.ofMinutes(30);

    /**
     * Seuil d'absence de position : si aucune nouvelle position n'est reçue
     * pendant cette durée, une alerte est levée (RG-066, EF-TRK-04).
     */
    private static final Duration SEUIL_ABSENCE_POSITION = Duration.ofHours(2);

    /**
     * Écart maximal toléré par rapport au corridor de l'axe (en km).
     * Au-delà, on considère que le véhicule a quitté l'axe prévu.
     * Valeur large pour le MVP (50 km) car les déviations sont fréquentes
     * sur un réseau peu revêtu.
     */
    private static final double SEUIL_ECART_CORRIDOR_KM = 50.0;

    /**
     * Distance maximale entre deux positions consécutives considérée comme
     * physiquement possible (en km). Au-delà, c'est une position aberrante.
     * 300 km en 5 minutes = 3600 km/h, impossible pour un camion.
     */
    private static final double SEUIL_SAUT_ABERRANT_KM = 50.0;

    /**
     * Détecte les anomalies à partir de l'historique des positions d'une mission.
     *
     * @param missionId       identifiant de la mission
     * @param positionsRécentes positions récentes ordonnées par horodatageCapture
     * @return résultat de détection (peut être vide si aucune anomalie)
     */
    public ResultatDetection detecter(UUID missionId, List<Position> positionsRécentes) {
        if (positionsRécentes == null || positionsRécentes.isEmpty()) {
            return ResultatDetection.aucuneAnomalie();
        }

        Position dernière = positionsRécentes.get(positionsRécentes.size() - 1);
        Instant maintenant = Instant.now();

        // 1. Détection absence prolongée de position
        Duration ageDernierePosition = Duration.between(dernière.getHorodatageCapture(), maintenant);
        boolean absenceProlongee = ageDernierePosition.compareTo(SEUIL_ABSENCE_POSITION) > 0;

        // 2. Détection arrêt prolongé
        boolean arretProlonge = detecterArretProlonge(positionsRécentes);

        // 3. Détection position aberrante (saut impossible)
        boolean positionAberrante = detecterSautAberrant(positionsRécentes);

        // 4. Détection écart de corridor (simplifié MVP : basé sur distance
        //    entre positions consécutives, pas encore sur GEO/OPT)
        boolean ecartCorridor = detecterEcartCorridor(positionsRécentes);

        boolean anomalieDetectee = absenceProlongee || arretProlonge || positionAberrante || ecartCorridor;

        if (anomalieDetectee) {
            log.info("Anomalie détectée - mission={}, absence={}, arrêt={}, aberrant={}, écart={}",
                    missionId, absenceProlongee, arretProlonge, positionAberrante, ecartCorridor);
        }

        return new ResultatDetection(
                anomalieDetectee,
                absenceProlongee,
                arretProlonge,
                positionAberrante,
                ecartCorridor,
                ageDernierePosition,
                dernière.getHorodatageCapture()
        );
    }

    /**
     * Détecte un arrêt prolongé : le véhicule a parcouru moins de
     * SEUIL_DISTANCE_ARRET_KM pendant SEUIL_DUREE_ARRET.
     */
    private boolean detecterArretProlonge(List<Position> positions) {
        if (positions.size() < 2) {
            return false;
        }

        // Fenêtre d'analyse : positions dans les SEUIL_DUREE_ARRET dernières minutes
        Instant seuilTemporal = Instant.now().minus(SEUIL_DUREE_ARRET);
        List<Position> fenetre = positions.stream()
                .filter(p -> p.getHorodatageCapture().isAfter(seuilTemporal))
                .toList();

        if (fenetre.size() < 2) {
            return false;
        }

        // Distance totale parcourue dans la fenêtre
        double distanceKm = 0.0;
        for (int i = 1; i < fenetre.size(); i++) {
            distanceKm += haversine(
                    fenetre.get(i - 1).getLatitude(), fenetre.get(i - 1).getLongitude(),
                    fenetre.get(i).getLatitude(), fenetre.get(i).getLongitude()
            );
        }

        return distanceKm < SEUIL_DISTANCE_ARRET_KM;
    }

    /**
     * Détecte un saut aberrant entre deux positions consécutives.
     * Un saut de plus de SEUIL_SAUT_ABERRANT_KM en moins de 5 minutes
     * est physiquement impossible pour un camion.
     */
    private boolean detecterSautAberrant(List<Position> positions) {
        if (positions.size() < 2) {
            return false;
        }

        // Vérifier les 2 dernières positions uniquement (temps réel)
        int n = positions.size();
        Position avantDerriere = positions.get(n - 2);
        Position derniere = positions.get(n - 1);

        double distanceKm = haversine(
                avantDerriere.getLatitude(), avantDerriere.getLongitude(),
                derniere.getLatitude(), derniere.getLongitude()
        );

        double tempsMinutes = Duration.between(
                avantDerriere.getHorodatageCapture(),
                derniere.getHorodatageCapture()
        ).toSeconds() / 60.0;

        if (tempsMinutes <= 0) {
            return false;
        }

        // Vitesse en km/h
        double vitesseKmh = distanceKm / (tempsMinutes / 60.0);

        // Aberrant si > 200 km/h (un camion ne dépasse pas 120 km/h,
        // mais on garde une marge pour le GPS imprécis)
        return vitesseKmh > 200.0;
    }

    /**
     * Détecte un écart au corridor (version MVP simplifiée).
     * Vérifie si le véhicule s'éloigne de plus de SEUIL_ECART_CORRIDOR_KM
     * de sa position précédente en ligne droite — indicateur grossier
     * qu'il a quitté l'axe prévu.
     *
     * En Phase 2, cette méthode utilisera le référentiel GEO (axes/hubs)
     * pour un calcul précis de distance au corridor.
     */
    private boolean detecterEcartCorridor(List<Position> positions) {
        // MVP : pas encore couplé à GEO, on se base sur la distance
        // entre la première et la dernière position de la fenêtre
        if (positions.size() < 5) {
            return false;
        }

        Position premiere = positions.get(0);
        Position derniere = positions.get(positions.size() - 1);

        double distanceKm = haversine(
                premiere.getLatitude(), premiere.getLongitude(),
                derniere.getLatitude(), derniere.getLongitude()
        );

        // Alerte si la distance parcourue depuis la première position
        // dépasse le seuil (indicateur d'un éloignement significatif)
        return distanceKm > SEUIL_ECART_CORRIDOR_KM;
    }

    /**
     * Formule de Haversine (identique à EtaCalculator — sera mutualisée
     * dans un utilitaire commun en Phase 2).
     */
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Résultat de la détection d'anomalies.
     */
    public record ResultatDetection(
            boolean anomalieDetectee,
            boolean absenceProlongee,
            boolean arretProlonge,
            boolean positionAberrante,
            boolean ecartCorridor,
            Duration ageDernierePosition,
            Instant horodatageDernierePosition
    ) {
        public static ResultatDetection aucuneAnomalie() {
            return new ResultatDetection(false, false, false, false, false,
                    Duration.ZERO, null);
        }

        public String description() {
            if (!anomalieDetectee) {
                return "Aucune anomalie";
            }
            StringBuilder sb = new StringBuilder("Anomalies: ");
            if (absenceProlongee) sb.append("absence prolongée, ");
            if (arretProlonge) sb.append("arrêt prolongé, ");
            if (positionAberrante) sb.append("position aberrante, ");
            if (ecartCorridor) sb.append("écart de corridor, ");
            // Enlever la dernière virgule
            if (sb.length() > 11) sb.setLength(sb.length() - 2);
            return sb.toString();
        }
    }
}
