package com.fretcorridor.gateway.domain.cap;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/** Port hexagonal : UC-MAT-02/diffusion-course, "mes propositions" côté app Chauffeur. */
public interface PropositionCapPort {

    Flux<PropositionCap> mesPropositions(String delegationToken);

    Mono<Void> accepter(UUID affectationId, UUID demandeId, UUID capaciteId, String delegationToken);

    Mono<Void> refuser(UUID affectationId, UUID demandeId, UUID capaciteId, String delegationToken);
}
