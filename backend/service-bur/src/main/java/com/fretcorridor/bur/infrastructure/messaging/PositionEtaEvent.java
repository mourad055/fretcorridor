package com.fretcorridor.bur.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Miroir exact du contrat shared-contracts/asyncapi/events/position-eta.yaml
 * (TRK, Moteur → EXE, Mobile — consommé ici aussi par Bureau/service-bur
 * pour matérialiser la vue Bureau des positions, RG-043/ENF-PRF-02).
 */
public record PositionEtaEvent(
        UUID eventId,
        UUID missionId,
        UUID vehiculeId,
        Double derniereLatitude,
        Double derniereLongitude,
        Instant horodatageDernierePosition,
        Double distanceRestanteKm,
        Double vitesseEstimeeKmh,
        Instant etaCentral,
        Instant etaBorneBasse,
        Instant etaBorneHaute,
        String sourceCapture,
        Instant horodatageCalcul
) {
}
