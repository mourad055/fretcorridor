package com.flysoft.fretcorridor.not.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Miroir du contrat service-trk (cf
 * shared-contracts/asyncapi/events/alerte-ecart.yaml) - copie locale
 * volontaire, pas de bibliotheque Java partagee entre porteurs.
 */
public record AlerteEcartEvent(
        UUID eventId,
        UUID missionId,
        UUID vehiculeId,
        String typeAnomalie,
        String description,
        double derniereLatitude,
        double derniereLongitude,
        Instant horodatageDernierePosition,
        long agePositionSecondes,
        String sourceCapture,
        Instant horodatageDetection
) {
}
