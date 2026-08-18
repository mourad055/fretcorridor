package com.fretcorridor.gateway.domain.flt;

import reactor.core.publisher.Mono;

/**
 * Envoi de positions GPS par le chauffeur (EF-TRK-01, Sprint 6). Utilise le
 * delegationToken de l'acteur — service-flt valide les JWT service-ida
 * (même principe que les autres services Mobile).
 */
public interface PositionPort {
    Mono<Void> envoyer(String delegationToken, PositionEnvoi position);
}
