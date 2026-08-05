package com.fretcorridor.dto;

/**
 * Point géographique (latitude, longitude) - SRID 4326 (WGS84).
 * Utilisé par GEO, OPT, TRK pour échanger des coordonnées GPS.
 */
public record PointGeoDto(double latitude, double longitude) {
    public PointGeoDto {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude invalide: " + latitude);
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude invalide: " + longitude);
        }
    }
}
