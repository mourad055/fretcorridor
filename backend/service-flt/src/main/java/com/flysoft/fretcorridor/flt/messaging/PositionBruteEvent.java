package com.flysoft.fretcorridor.flt.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Miroir du contrat consomme par service-trk (cf
 * shared-contracts/asyncapi/events/position-brute.yaml) - copie locale
 * volontaire, pas de bibliotheque Java partagee entre porteurs (meme
 * principe que les autres evenements du depot).
 */
public record PositionBruteEvent(
        UUID eventId,
        UUID missionId,
        UUID vehiculeId,
        double latitude,
        double longitude,
        String sourceCapture,
        Double precisionMetres,
        Instant horodatageCapture,
        Instant horodatageTransmission
) {
}
