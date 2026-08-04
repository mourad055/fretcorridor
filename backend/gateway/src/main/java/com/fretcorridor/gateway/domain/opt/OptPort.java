package com.fretcorridor.gateway.domain.opt;

import reactor.core.publisher.Flux;

/**
 * Port hexagonal : le domaine ignore que l'implémentation actuelle est un mock.
 * TODO(moteur): remplacer par l'appel réel à service-opt une fois ce service
 * livré (@stevetelecom, issue #21).
 */
public interface OptPort {

    /** ENF-MUL-01 : ne restitue jamais que les missions du tenant demandé. */
    Flux<MissionAppariee> listerMissionsParTenant(String tenantId);
}
