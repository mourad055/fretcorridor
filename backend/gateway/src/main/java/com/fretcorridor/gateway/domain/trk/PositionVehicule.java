package com.fretcorridor.gateway.domain.trk;

import java.time.Instant;

/**
 * Vue minimale d'une position telle qu'exposée à la supervision du Bureau
 * (Sprint 6). Le suivi complet (ETA dynamique, détection d'anomalies)
 * appartient à service-trk (Moteur, CDC EF-TRK-01/02/03).
 *
 * RG-043 (CDC) : aucune position n'est affichée sans son âge — {@code capturedLe}
 * est la seule source de vérité, l'âge se calcule à la restitution, jamais stocké.
 */
public record PositionVehicule(
        String id,
        String tenantId,
        String vehiculeLabel,
        double latitude,
        double longitude,
        Instant capturedLe
) {
}
