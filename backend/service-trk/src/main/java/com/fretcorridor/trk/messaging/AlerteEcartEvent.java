package com.fretcorridor.trk.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload Kafka de l'événement AlerteEcart (TRK → service-not, Mobile).
 *
 * Publié lorsqu'une anomalie est détectée par AnomalieDetector.
 * Types d'anomalies : arrêt prolongé, absence de position, saut aberrant,
 * écart de corridor (EF-TRK-03).
 *
 * Consommé par service-not (Mobile) pour notification multicanal.
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
