package com.fretcorridor.gateway.domain.cap;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Déclaration de capacité par le chauffeur/transporteur (EF-CAP-03/07,
 * Sprint 4) — appel réel à service-cap, séparé de CapacitePort (encore mocké
 * : pas d'équivalent GET côté service-cap, cf. MockCapAdapter/vue Bureau).
 */
public interface CapaciteDeclarationPort {
    Mono<CapaciteDeclaree> declarer(DeclarationCapacite requete, String delegationToken);

    // "Mes capacités" — liste des déclarations du transporteur connecté.
    Flux<CapaciteDeclaree> mesCapacites(String delegationToken);

    Mono<Void> supprimer(String capaciteId, String delegationToken);
}
