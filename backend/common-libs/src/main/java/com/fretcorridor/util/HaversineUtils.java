package com.fretcorridor.util;

import com.fretcorridor.dto.PointGeoDto;

/**
 * Utilitaires de calcul géographique.
 * Centralise la formule de Haversine pour éviter la duplication
 * entre EtaCalculator et AnomalieDetector dans TRK.
 */
public final class HaversineUtils {

    private static final double RAYON_TERRE_KM = 6371.0;

    private HaversineUtils() {
        // Classe utilitaire - pas d'instanciation
    }

    /**
     * Distance en km entre deux points GPS (formule de Haversine).
     */
    public static double distance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return RAYON_TERRE_KM * c;
    }

    public static double distance(PointGeoDto p1, PointGeoDto p2) {
        return distance(p1.latitude(), p1.longitude(), p2.latitude(), p2.longitude());
    }
}
