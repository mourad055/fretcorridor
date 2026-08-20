package com.fretcorridor.pay.domain;

import java.time.Instant;

/**
 * EF-PAY-08 : état du litige (dossier ADM de type LITIGE) le plus récent
 * connu pour une mission, matérialisé depuis l'événement Kafka
 * {@code dossier-litige} publié par service-adm — même principe que
 * {@code PositionVehicule} côté service-bur (ADR 0014) : upsert-si-plus-
 * récent, pas d'unicité stricte sur un identifiant d'événement.
 */
public record LitigeMission(String missionId, String tenantId, boolean actif, Instant horodatage) {
}
