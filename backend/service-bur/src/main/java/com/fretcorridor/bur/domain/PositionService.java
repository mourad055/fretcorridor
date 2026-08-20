package com.fretcorridor.bur.domain;

import java.util.List;

/** Domaine pur, sans dépendance à Spring — testable sans mock de framework (PRD §8.1). */
public class PositionService {

    private final PositionRepositoryPort repository;

    public PositionService(PositionRepositoryPort repository) {
        this.repository = repository;
    }

    public void ingerer(PositionVehicule position) {
        repository.enregistrerSiPlusRecente(position);
    }

    public List<PositionVehicule> listerParTenant(String tenantId) {
        return repository.listerParTenant(tenantId);
    }
}
