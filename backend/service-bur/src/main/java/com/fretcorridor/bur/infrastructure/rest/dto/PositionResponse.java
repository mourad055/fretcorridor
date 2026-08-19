package com.fretcorridor.bur.infrastructure.rest.dto;

import com.fretcorridor.bur.domain.PositionVehicule;

import java.time.Instant;
import java.util.UUID;

public record PositionResponse(
        UUID missionId,
        UUID vehiculeId,
        double latitude,
        double longitude,
        Instant capturedLe
) {
    public static PositionResponse from(PositionVehicule position) {
        return new PositionResponse(
                position.missionId(),
                position.vehiculeId(),
                position.latitude(),
                position.longitude(),
                position.capturedLe()
        );
    }
}
