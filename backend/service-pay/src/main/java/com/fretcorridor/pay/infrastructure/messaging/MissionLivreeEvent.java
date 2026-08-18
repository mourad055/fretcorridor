package com.fretcorridor.pay.infrastructure.messaging;

import java.time.Instant;

/**
 * Miroir exact du contrat publié par service-exe
 * (shared-contracts/asyncapi/events/mission-livree.yaml, PR #79) —
 * {@code transporteurId} est nullable au schéma (pas garanti par le
 * producteur), {@code preuveLivraisonReference} est une référence opaque :
 * service-pay n'en valide que la présence (RG-078), jamais la nature.
 */
public record MissionLivreeEvent(
        String eventId,
        String missionId,
        String tenantId,
        String transporteurId,
        String preuveLivraisonReference,
        Instant dateLivraison
) {
}
