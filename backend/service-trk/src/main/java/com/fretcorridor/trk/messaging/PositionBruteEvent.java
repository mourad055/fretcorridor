package com.fretcorridor.trk.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload Kafka de l'evenement PositionBrute (cf shared-contracts/asyncapi/events/position-brute.yaml).
 * BROUILLON - contrat non encore valide avec Personne 1 (Mobile, service-flt).
 *
 * Volontairement distinct de l'entite Position (persistance) : le contrat
 * d'echange et le modele de stockage evoluent independamment.
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
