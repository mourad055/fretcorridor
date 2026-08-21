package com.fretcorridor.gateway.domain.flt;

/**
 * EF-TRK-01 (Sprint 6) : position GPS capturée côté chauffeur, envoyée à service-flt.
 *
 * FIX audit 21/08 : eventId (généré par l'app à la capture, ENF-SEC-03),
 * sourceCapture (enum contrat position-brute.yaml) et precisionMetres sont
 * optionnels et transparents - null => comportement historique côté flt.
 */
public record PositionEnvoi(String missionId, double latitude, double longitude, String horodatage,
                            String eventId, String sourceCapture, Double precisionMetres) {
}
