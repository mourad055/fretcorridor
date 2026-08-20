package com.fretcorridor.gateway.infrastructure.rest.geo.dto;

import com.fretcorridor.gateway.domain.geo.Axe;

/** EF-GEO-03 : les 3 etats restent independants jusqu'au client Angular. */
public record AxeResponse(
        String id,
        String origine,
        String destination,
        double distanceKm,
        boolean visibiliteActive,
        boolean matchingActif,
        boolean paiementActif
) {
    public static AxeResponse from(Axe axe) {
        return new AxeResponse(
                axe.id(),
                axe.origine(),
                axe.destination(),
                axe.distanceKm(),
                axe.visibiliteActive(),
                axe.matchingActif(),
                axe.paiementActif()
        );
    }
}
