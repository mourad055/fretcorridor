package com.fretcorridor.gateway.domain.geo;

import reactor.core.publisher.Flux;

/**
 * Port hexagonal : le domaine ignore que l'implémentation actuelle est un mock.
 * TODO(moteur): remplacer par l'appel réel à service-geo une fois ce service livré
 * (Sprint 3 Moteur, @stevetelecom, issue #21).
 */
public interface GeoPort {

    /** ENF-MUL-01 : ne restitue jamais que les axes du tenant demandé. */
    Flux<Axe> listerAxesParTenant(String tenantId);
}
