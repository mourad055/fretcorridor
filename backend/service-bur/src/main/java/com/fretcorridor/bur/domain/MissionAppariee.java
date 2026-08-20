package com.fretcorridor.bur.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Vue Bureau d'une affectation confirmée par le Moteur — matérialisée en
 * consommant l'événement Kafka AffectationConfirmee (OPT → EXE, Mobile),
 * jamais par appel REST direct à service-opt (opt-api.yaml l'interdit
 * explicitement : "aucun consommateur cross-porteur n'appelle cette API
 * directement — le flux OPT -> Mobile passe exclusivement par événements
 * Kafka asynchrones").
 *
 * {@code tenantId} : l'événement ne porte pas de tenant (Phase 1 mono-tenant,
 * même raisonnement que l'ADR 0011 pour GEO) — stampé à l'ingestion depuis
 * la configuration, pas déduit de l'événement lui-même.
 *
 * Pas de statut de cycle de vie (EN_COURS/CLOTUREE) : aucun événement ne
 * porte encore cette information (ni AffectationConfirmee, ni aucun autre
 * contrat de shared-contracts/asyncapi/ à ce jour) — cf.
 * docs/ROADMAP_INTEGRATION_gateway.md.
 */
public record MissionAppariee(
        UUID missionId,
        String tenantId,
        UUID axeId,
        UUID transporteurId,
        String origineNom,
        String destinationNom,
        BigDecimal prixTransport,
        String devise,
        Instant confirmeeLe
) {
}
