package com.fretcorridor.gateway.domain.exe;

import java.util.List;

public record MissionExecutionDetail(String missionId, String statut, List<EtapeExecution> etapes) {
}
