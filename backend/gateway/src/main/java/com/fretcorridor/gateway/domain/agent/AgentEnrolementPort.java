package com.fretcorridor.gateway.domain.agent;

import reactor.core.publisher.Mono;

/**
 * Enrôlement assisté par agent de terrain (RG-019) : le delegationToken porte
 * l'identité de l'agent, jamais celle de la personne enrôlée — service-ida ne
 * renvoie d'ailleurs aucun jeton pour le compte créé à l'activation.
 */
public interface AgentEnrolementPort {

    Mono<Enrolement> initier(String delegationToken, String telephone, String typeActeur,
                              double latitude, double longitude, String idempotencyKey);

    Mono<Enrolement> activer(String delegationToken, String enrolementId, String otp, String codePin);
}
