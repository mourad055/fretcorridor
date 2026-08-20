package com.fretcorridor.bur.domain;

/**
 * Port hexagonal : le domaine ignore l'implémentation JPA/Postgres.
 * TODO: l'enregistrement sera déclenché par consommation d'événements Kafka
 * (PropositionEmise/AffectationConfirmee, cf. Plan d'Exécution §4.3) plutôt que
 * par appel REST direct, une fois le bus câblé pour ce service.
 */
public interface MissionRepositoryPort {

    void enregistrer(String tenantId, String axeId);

    long compter(String tenantId, String axeId);
}
