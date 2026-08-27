package com.fretcorridor.gateway.domain.cap;

import reactor.core.publisher.Flux;

/**
 * Port hexagonal : lecture des capacités déclarées côté vue Transporteur web
 * (FE-TRP-01). Appel réel à service-cap via delegationToken — le filtre
 * transporteur × tenant est appliqué par service-cap depuis le JWT service-ida.
 */
public interface CapacitePort {

    /** Périmètre strict par acteur (PRD §5.3) : jamais la capacité d'un autre transporteur. */
    Flux<Capacite> listerMesCapacites(String transporteurId, String delegationToken);
}
