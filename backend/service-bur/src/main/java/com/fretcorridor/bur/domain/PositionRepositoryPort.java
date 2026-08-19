package com.fretcorridor.bur.domain;

import java.util.List;

/** Port hexagonal : le domaine ignore l'implémentation JPA/Postgres. */
public interface PositionRepositoryPort {

    /**
     * Upsert par missionId — contrairement à MissionAppariee (append-only,
     * idempotent par eventId), une position REMPLACE la précédente pour la
     * même mission. N'écrase jamais une position plus récente par une plus
     * ancienne (Kafka ne garantit pas l'ordre entre partitions) — un rejeu
     * ou message en retard est silencieusement ignoré, jamais une erreur.
     */
    void enregistrerSiPlusRecente(PositionVehicule position);

    List<PositionVehicule> listerParTenant(String tenantId);
}
