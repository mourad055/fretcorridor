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
        String tenantId,
        double distanceKm,
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
                distanceKm(axe.getHubOrigine().getPosition(), axe.getHubDestination().getPosition()),
                axe.getDateCreation()
        );
    }

    // Haversine (rayon terrestre moyen 6371 km) : suffisant pour un ordre de
    // grandeur affiche a l'utilisateur (EF-GEO-01), pas pour un calcul
    // d'itineraire routier reel (hors perimetre, cf Valhalla en Phase
    // ulterieure). Point JTS : x = longitude, y = latitude (SRID 4326).
    private static double distanceKm(org.locationtech.jts.geom.Point origine, org.locationtech.jts.geom.Point destination) {
        double lat1 = Math.toRadians(origine.getY());
        double lat2 = Math.toRadians(destination.getY());
        double deltaLat = Math.toRadians(destination.getY() - origine.getY());
        double deltaLon = Math.toRadians(destination.getX() - origine.getX());

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371.0 * c;
    }
}
