package com.fretcorridor.gateway.infrastructure.rest.exe.dto;

import com.fretcorridor.gateway.domain.exe.Mission;

import java.util.List;

public record MissionResponse(
        String id,
        String transporteurNom,
        String origine,
        String destination,
        List<EtapeMissionResponse> etapes
) {
    public static MissionResponse from(Mission mission) {
        return new MissionResponse(
                mission.id(),
                mission.transporteurNom(),
                mission.origine(),
                mission.destination(),
                mission.etapes().stream().map(EtapeMissionResponse::from).toList()
        );
    }
}
