package com.fretcorridor.gateway.infrastructure.rest.trk.dto;

import com.fretcorridor.gateway.domain.trk.PositionVehicule;

import java.time.Duration;
import java.time.Instant;

/**
 * RG-043 : {@code ageSecondes} est calculé à chaque restitution, jamais stocké —
 * ce champ n'est jamais nul, une position n'existe pas sans âge calculable.
 */
public record PositionResponse(
        String id,
        String vehiculeLabel,
        double latitude,
        double longitude,
        Instant capturedLe,
        long ageSecondes
) {
    public static PositionResponse from(PositionVehicule position) {
        long age = Duration.between(position.capturedLe(), Instant.now()).getSeconds();
        return new PositionResponse(
                position.id(),
                position.vehiculeLabel(),
                position.latitude(),
                position.longitude(),
                position.capturedLe(),
                Math.max(age, 0)
        );
    }
}
