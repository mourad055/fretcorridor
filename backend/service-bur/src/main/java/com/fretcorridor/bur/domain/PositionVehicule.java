package com.fretcorridor.bur.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Dernière position connue d'un véhicule en mission — matérialisée en
 * consommant l'événement Kafka PositionETA (TRK → EXE, Mobile), jamais par
 * appel REST direct à service-trk (qui n'expose d'ailleurs aucune API REST :
 * "Aucune API REST publique (tout est événementiel)", cf. son propre README).
 *
 * {@code vehiculeLabel} : l'événement ne porte qu'un vehiculeId (UUID), pas
 * de libellé lisible ("Camion 10T — LT 1234 AB") — le résoudre exigerait un
 * appel supplémentaire à service-flt, hors périmètre de cette décision (même
 * limite que transporteurNom pour MissionAppariee, cf. ADR 0013).
 *
 * RG-043 (CDC) : aucune position n'est affichée sans son âge — {@code capturedLe}
 * est la seule source de vérité, l'âge se calcule à la restitution, jamais stocké.
 */
public record PositionVehicule(
        UUID missionId,
        String tenantId,
        UUID vehiculeId,
        double latitude,
        double longitude,
        Instant capturedLe
) {
}
