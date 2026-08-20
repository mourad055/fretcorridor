package com.fretcorridor.gateway.domain.opt;

import java.time.Instant;

/**
 * Vue minimale d'une mission issue de l'appariement, telle qu'exposée à la
 * supervision du Bureau (Sprint 5). Le cycle d'appariement complet (coût
 * composite, traçabilité de la décision) appartient à service-mat/service-opt
 * (Moteur, CDC §8).
 */
public record MissionAppariee(
        String id,
        String tenantId,
        String axeId,
        String transporteurNom,
        String origine,
        String destination,
        Instant enlevementLe,
        StatutMission statut
) {
}
