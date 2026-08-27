package com.fretcorridor.gateway.domain.opt;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * UC-MAT-02 du CDC. ATTENTION (26/08) : l'implementation reelle
 * (RealPropositionMissionAdapter) appelle service-opt en REST direct, ce que
 * ADR 0013/opt-api.yaml interdisent explicitement pour tout consommateur
 * cross-porteur -- voir le commentaire complet sur
 * RealPropositionMissionAdapter et fretcorridor.client.service-opt
 * (application.yml). Entorse temporaire assumee pour livrer une demo
 * fonctionnelle, PAS a pousser sur dev sans regularisation.
 */
public interface PropositionMissionPort {
    Flux<PropositionMissionCandidate> mesPropositions(UUID transporteurId);

    Mono<PropositionMissionCandidate> accepter(UUID propositionId, UUID transporteurId);

    Mono<PropositionMissionCandidate> refuser(UUID propositionId, UUID transporteurId, String motif);
}
