package com.fretcorridor.gateway.infrastructure.rest.exe.dto;

import com.fretcorridor.gateway.domain.exe.MissionExecution;

public record MissionExecutionResponse(String missionId, String statut, String origineNom, String destinationNom,
                                        String dateCreation, String tourneeId) {
    public static MissionExecutionResponse from(MissionExecution m) {
        return new MissionExecutionResponse(m.missionId(), m.statut(), m.origineNom(), m.destinationNom(),
                m.dateCreation(), m.tourneeId());
    }
}
