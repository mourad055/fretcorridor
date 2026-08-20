package com.fretcorridor.gateway.domain.exe;

import reactor.core.publisher.Flux;

/**
 * Port hexagonal : le domaine ignore que l'implémentation actuelle est un mock.
 * TODO(mobile): remplacer par l'appel réel à service-exe une fois ce service
 * livré (@estie-glo, issue #16).
 */
public interface ExePort {

    /** ENF-MUL-01 : supervision Bureau, jamais que le territoire du tenant demandé. */
    Flux<Mission> listerMissionsParTenant(String tenantId);

    /** PRD §5.3 : périmètre strict par acteur, jamais la mission d'un autre transporteur. */
    Flux<Mission> listerMissionsParTransporteur(String transporteurId);
}
