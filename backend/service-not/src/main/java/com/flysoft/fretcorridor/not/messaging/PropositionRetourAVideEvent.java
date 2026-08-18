package com.flysoft.fretcorridor.not.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Miroir du contrat côté service-opt (Moteur) —
 * shared-contracts/asyncapi/events/proposition-retour-a-vide.yaml.
 *
 * tourneeId/affectationId : mutuellement exclusifs, jamais les deux
 * présents, jamais les deux absents (documenté dans le contrat). Ne jamais
 * supposer que l'un des deux est toujours renseigné.
 */
public record PropositionRetourAVideEvent(
        UUID eventId,
        UUID tourneeId,
        UUID affectationId,
        UUID capaciteId,
        UUID axeId,
        double pointDepartLatitude,
        double pointDepartLongitude,
        Instant dateGeneration
) {
}
