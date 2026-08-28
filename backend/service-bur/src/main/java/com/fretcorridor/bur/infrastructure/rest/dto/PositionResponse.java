package com.fretcorridor.bur.infrastructure.rest.dto;

import com.fretcorridor.bur.domain.PositionVehicule;

import java.time.Instant;
import java.util.UUID;

public record PositionResponse(
        UUID missionId,
        UUID vehiculeId,
        String libelle,
        double latitude,
        double longitude,
        Instant capturedLe
) {
    public static PositionResponse from(PositionVehicule position) {
        return from(position, position.vehiculeId() != null
                ? position.vehiculeId().toString()
                : position.missionId().toString());
    }

    public static PositionResponse from(PositionVehicule position, String libelle) {
        return new PositionResponse(
                position.missionId(),
                position.vehiculeId(),
                libelle,
                position.latitude(),
                position.longitude(),
                position.capturedLe()
        );
    }
}
