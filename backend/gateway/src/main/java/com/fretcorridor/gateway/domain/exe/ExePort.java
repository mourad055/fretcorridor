package com.fretcorridor.gateway.domain.exe;

import reactor.core.publisher.Flux;

/**
 * Port hexagonal : chronologie mission côté vue web Bureau/Transporteur (Sprint 7).
 * Bureau : missions matérialisées par service-bur ; Transporteur : service-exe.
 */
public interface ExePort {

    /** ENF-MUL-01 : supervision Bureau, jamais que le territoire du tenant demandé. */
    Flux<Mission> listerMissionsParTenant(String tenantId, String delegationToken);

    /** PRD §5.3 : périmètre strict par acteur, jamais la mission d'un autre transporteur. */
    Flux<Mission> listerMissionsParTransporteur(String tenantId, String transporteurId, String delegationToken);
}
