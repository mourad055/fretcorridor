package com.fretcorridor.geo.web.dto;

import com.fretcorridor.geo.domain.Axe;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AxeResponse(
        UUID id,
        String nom,
        UUID hubOrigineId,
        String hubOrigineNom,
        UUID hubDestinationId,
        String hubDestinationNom,
        boolean visibiliteActive,
        boolean matchingActif,
        boolean paiementActif,
        Map<String, Object> parametres,
        Instant dateCreation
) {
    public static AxeResponse from(Axe axe) {
        return new AxeResponse(
                axe.getId(),
                axe.getNom(),
                axe.getHubOrigine().getId(),
                axe.getHubOrigine().getNom(),
                axe.getHubDestination().getId(),
                axe.getHubDestination().getNom(),
                axe.isVisibiliteActive(),
                axe.isMatchingActif(),
                axe.isPaiementActif(),
                axe.getParametres(),
                axe.getDateCreation()
        );
    }
}
