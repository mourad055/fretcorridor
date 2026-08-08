package com.fretcorridor.trk.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload Kafka de l'événement PositionETA (TRK → service-exe, Mobile).
 *
 * Publié après chaque ingestion de PositionBrute et recalcul de l'ETA.
 * Contient l'ETA avec son intervalle de confiance (RG-067).
 *
 * Consommé par service-exe (Mobile) pour l'écran de suivi client.
 */
public record PositionEtaEvent(
        UUID eventId,
        UUID missionId,
        UUID vehiculeId,
        double derniereLatitude,
        double derniereLongitude,
        Instant horodatageDernierePosition,
        double distanceRestanteKm,
        double vitesseEstimeeKmh,
        Instant etaCentral,
        Instant etaBorneBasse,
        Instant etaBorneHaute,
        String sourceCapture,
        Instant horodatageCalcul
) {
}
