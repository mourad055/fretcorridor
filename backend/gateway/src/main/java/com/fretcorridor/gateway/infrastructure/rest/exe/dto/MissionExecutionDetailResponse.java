package com.fretcorridor.gateway.infrastructure.rest.exe.dto;

import com.fretcorridor.gateway.domain.exe.EtapeExecution;
import com.fretcorridor.gateway.domain.exe.MissionExecutionDetail;
import java.util.List;

public record MissionExecutionDetailResponse(String missionId, String statut, List<EtapeExecutionResponse> etapes) {
    public static MissionExecutionDetailResponse from(MissionExecutionDetail d) {
        return new MissionExecutionDetailResponse(d.missionId(), d.statut(),
                d.etapes().stream().map(EtapeExecutionResponse::from).toList());
    }

    public record EtapeExecutionResponse(String type, String libelle, String horodatageCapture, String horodatageTransmission) {
        public static EtapeExecutionResponse from(EtapeExecution e) {
            return new EtapeExecutionResponse(e.type(), e.libelle(), e.horodatageCapture(), e.horodatageTransmission());
        }
    }
}
