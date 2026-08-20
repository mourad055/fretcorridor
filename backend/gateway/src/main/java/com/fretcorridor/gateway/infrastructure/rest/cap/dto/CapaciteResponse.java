package com.fretcorridor.gateway.infrastructure.rest.cap.dto;

import com.fretcorridor.gateway.domain.cap.Capacite;

import java.time.Instant;

public record CapaciteResponse(
        String id,
        String vehicule,
        String origine,
        String destination,
        Instant departLe,
        double poidsTaxableKg,
        String modeCollecte,
        String etat
) {
    public static CapaciteResponse from(Capacite capacite) {
        return new CapaciteResponse(
                capacite.id(),
                capacite.vehicule(),
                capacite.origine(),
                capacite.destination(),
                capacite.departLe(),
                capacite.poidsTaxableKg(),
                capacite.modeCollecte().name(),
                capacite.etat().name()
        );
    }
}
