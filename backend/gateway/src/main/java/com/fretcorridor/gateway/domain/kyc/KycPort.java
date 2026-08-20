package com.fretcorridor.gateway.domain.kyc;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Port hexagonal : le domaine ignore que l'implémentation actuelle est un mock.
 * TODO(mobile): remplacer par l'appel réel à service-ida une fois ce service livré.
 */
public interface KycPort {

    Flux<KycDossier> listerEnAttente();

    Mono<KycDossier> decider(String dossierId, KycStatut decision, String idempotencyKey);
}
