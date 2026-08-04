package com.fretcorridor.gateway.domain.geo;

/**
 * Vue minimale d'un axe telle qu'exposée à la carte du Bureau (Sprint 3). Le
 * référentiel complet (hubs, zonage H3, surcouche de risque) appartient à
 * service-geo (Moteur, CDC §9.9) — ce dashboard n'en consomme qu'un sous-ensemble.
 */
public record Axe(
        String id,
        String tenantId,
        String origine,
        String destination,
        double distanceKm,
        AxeEtatActivation etatActivation
) {
}
