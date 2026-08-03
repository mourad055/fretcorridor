package com.fretcorridor.geo.web.dto;

import com.fretcorridor.geo.domain.Hub;
import com.fretcorridor.geo.domain.TypeHub;

import java.time.Instant;
import java.util.UUID;

public record HubResponse(
        UUID id,
        String nom,
        String ville,
        TypeHub typeHub,
        double latitude,
        double longitude,
        Instant dateCreation
) {
    public static HubResponse from(Hub hub) {
        return new HubResponse(
                hub.getId(),
                hub.getNom(),
                hub.getVille(),
                hub.getTypeHub(),
                hub.getPosition().getY(), // latitude
                hub.getPosition().getX(), // longitude
                hub.getDateCreation()
        );
    }
}
