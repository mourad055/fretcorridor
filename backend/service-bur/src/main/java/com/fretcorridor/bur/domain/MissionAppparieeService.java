package com.fretcorridor.bur.domain;

import java.util.List;
import java.util.UUID;

/** Domaine pur, sans dépendance à Spring — testable sans mock de framework (PRD §8.1). */
public class MissionAppparieeService {

    private final MissionAppparieeRepositoryPort repository;

    public MissionAppparieeService(MissionAppparieeRepositoryPort repository) {
        this.repository = repository;
    }

    public void ingerer(MissionAppariee mission, UUID eventId) {
        repository.enregistrer(mission, eventId);
    }

    public List<MissionAppariee> listerParTenant(String tenantId) {
        return repository.listerParTenant(tenantId);
    }
}
