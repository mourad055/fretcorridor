package com.fretcorridor.gateway.domain.flt;

/** EF-TRK-01 (Sprint 6) : position GPS capturée côté chauffeur, envoyée à service-flt. */
public record PositionEnvoi(String missionId, double latitude, double longitude, String horodatage) {
}
