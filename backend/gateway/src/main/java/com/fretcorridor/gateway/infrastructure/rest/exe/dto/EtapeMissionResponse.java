package com.fretcorridor.gateway.infrastructure.rest.exe.dto;

import com.fretcorridor.gateway.domain.exe.EtapeMission;

public record EtapeMissionResponse(int rang, String type, String lieu, String etat) {
    public static EtapeMissionResponse from(EtapeMission etape) {
        return new EtapeMissionResponse(etape.rang(), etape.type().name(), etape.lieu(), etape.etat().name());
    }
}
