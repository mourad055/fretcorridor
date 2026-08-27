package com.fretcorridor.gateway.domain.flt;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** S10 : console de flotte simplifiée — appel réel à service-flt. */
public interface VehiculePort {
    Mono<Vehicule> declarer(String delegationToken, DeclarationVehicule declaration);

    Flux<Vehicule> mesVehicules(String delegationToken);

    // CRUD véhicule (retour utilisatrice 21/08).
    Mono<Vehicule> modifier(String delegationToken, String vehiculeId, DeclarationVehicule declaration);

    Mono<Void> supprimer(String delegationToken, String vehiculeId);
}
