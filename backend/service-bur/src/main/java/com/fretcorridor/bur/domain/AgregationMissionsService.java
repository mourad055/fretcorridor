package com.fretcorridor.bur.domain;

/** Domaine pur, sans dépendance à Spring — testable sans mock de framework (PRD §8.1). */
public class AgregationMissionsService {

    private final MissionRepositoryPort repository;
    private final long seuilAgregation;

    public AgregationMissionsService(MissionRepositoryPort repository, long seuilAgregation) {
        if (seuilAgregation < 1) {
            throw new IllegalArgumentException("Le seuil d'agrégation doit être au moins 1");
        }
        this.repository = repository;
        this.seuilAgregation = seuilAgregation;
    }

    public void enregistrerMission(String tenantId, String axeId) {
        repository.enregistrer(tenantId, axeId);
    }

    public AgregatMissions agregatPourAxe(String tenantId, String axeId) {
        long comptage = repository.compter(tenantId, axeId);
        return AgregatMissions.depuisComptage(axeId, comptage, seuilAgregation);
    }
}
