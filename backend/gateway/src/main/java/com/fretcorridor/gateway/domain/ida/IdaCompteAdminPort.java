package com.fretcorridor.gateway.domain.ida;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

/** Port hexagonal : le domaine ignore que l'implémentation actuelle appelle service-ida (Mobile). */
public interface IdaCompteAdminPort {

    Flux<CompteAdmin> listerParTenant(String tenantId, String delegationToken);

    Mono<CompteAdmin> changerStatut(String compteId, String tenantId, boolean actif, String delegationToken);

    Mono<CompteAdmin> changerRoles(String compteId, String tenantId, Set<String> roles, String delegationToken);
}
