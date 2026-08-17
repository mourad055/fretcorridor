package com.fretcorridor.opt.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * EF-MAT-08 / RG-058 (Sprint 12, CDC S8.6). Publie par OPT une fois une
 * Tournee TERMINEE (toutes etapes EXECUTEE) - jamais avant, cf
 * Tournee.marquerTermineeSiToutesEtapesExecutees(). Contrat :
 * shared-contracts/asyncapi/events/proposition-retour-a-vide.yaml.
 */
public record PropositionRetourAVideEvent(
        UUID eventId,
        UUID tourneeId,
        UUID capaciteId,
        UUID axeId,
        double pointDepartLatitude,
        double pointDepartLongitude,
        Instant dateGeneration
) {
}
