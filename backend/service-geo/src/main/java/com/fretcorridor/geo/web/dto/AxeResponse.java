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
        String hubOrigineVille,
        UUID hubDestinationId,
        String hubDestinationNom,
        String hubDestinationVille,
        boolean visibiliteActive,
        boolean matchingActif,
        boolean paiementActif,
        Map<String, Object> parametres,
        UUID tenantId,
        Instant dateCreation
) {
    public static AxeResponse from(Axe axe) {
        return new AxeResponse(
                axe.getId(),
                axe.getNom(),
                axe.getHubOrigine().getId(),
                axe.getHubOrigine().getNom(),
                axe.getHubOrigine().getVille(),
                axe.getHubDestination().getId(),
                axe.getHubDestination().getNom(),
                axe.getHubDestination().getVille(),
                axe.isVisibiliteActive(),
                axe.isMatchingActif(),
                axe.isPaiementActif(),
                axe.getParametres(),
                axe.getTenantId(),
                axe.getDateCreation()
        );
    }
}
