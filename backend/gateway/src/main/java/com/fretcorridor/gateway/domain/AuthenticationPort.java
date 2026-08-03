package com.fretcorridor.gateway.domain;

import reactor.core.publisher.Mono;

/**
 * Port hexagonal : le domaine ne connaît que ce contrat, jamais son implémentation.
 * L'implémentation réelle (appel à service-ida via le bus/API Mobile) n'existe pas
 * encore — un adaptateur mock la remplace en attendant (cf. PRD §1.3, règle §10.4).
 */
public interface AuthenticationPort {

    Mono<Actor> authenticate(String phone, String code);
}
