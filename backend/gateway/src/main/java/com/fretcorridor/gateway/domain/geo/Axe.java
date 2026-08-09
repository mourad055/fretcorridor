package com.fretcorridor.gateway.domain.geo;

/**
 * Vue minimale d'un axe telle qu'exposée à la carte du Bureau (Sprint 3). Le
 * référentiel complet (hubs, zonage H3, surcouche de risque) appartient à
 * service-geo (Moteur, CDC §9.9) — ce dashboard n'en consomme qu'un sous-ensemble.
 *
 * EF-GEO-03 (MVP, priorité M) : les 3 etats sont INDEPENDANTS, jamais un
 * enum a valeur unique - un axe reel peut avoir matching ET paiement actifs
 * simultanement (verifie sur service-geo, axe Douala-Yaounde en Phase 1).
 * Un enum a valeur unique perdrait cette information et violerait EF-GEO-03.
 */
public record Axe(
        String id,
        String tenantId,
        String origine,
        String destination,
        double distanceKm,
        boolean visibiliteActive,
        boolean matchingActif,
        boolean paiementActif
) {
}
