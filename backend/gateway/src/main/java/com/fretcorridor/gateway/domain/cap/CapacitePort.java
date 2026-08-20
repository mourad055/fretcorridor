package com.fretcorridor.gateway.domain.cap;

import reactor.core.publisher.Flux;

/**
 * Port hexagonal : le domaine ignore que l'implémentation actuelle est un mock.
 * TODO(mobile): remplacer par l'appel réel à service-cap/service-mkt une fois ces
 * services livrés (Personne 1).
 */
public interface CapacitePort {

    /** Périmètre strict par acteur (PRD §5.3) : jamais la capacité d'un autre transporteur. */
    Flux<Capacite> listerParTransporteur(String transporteurId);
}
