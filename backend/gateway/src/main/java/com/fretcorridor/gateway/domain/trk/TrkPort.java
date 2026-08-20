package com.fretcorridor.gateway.domain.trk;

import reactor.core.publisher.Flux;

/**
 * Port hexagonal : le domaine ignore que l'implémentation actuelle est un mock.
 * TODO(moteur): remplacer par l'appel réel à service-trk une fois ce service
 * livré (@stevetelecom, issue #21).
 */
public interface TrkPort {

    /** ENF-MUL-01 : ne restitue jamais que les positions du tenant demandé. */
    Flux<PositionVehicule> listerPositionsParTenant(String tenantId, String delegationToken);
}
