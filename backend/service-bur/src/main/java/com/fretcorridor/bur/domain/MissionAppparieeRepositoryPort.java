package com.fretcorridor.bur.domain;

import java.util.List;
import java.util.UUID;

/** Port hexagonal : le domaine ignore l'implémentation JPA/Postgres. */
public interface MissionAppparieeRepositoryPort {

    /** Idempotent — un eventId déjà ingéré ne doit jamais créer de doublon. */
    void enregistrer(MissionAppariee mission, UUID eventId);

    List<MissionAppariee> listerParTenant(String tenantId);
}
