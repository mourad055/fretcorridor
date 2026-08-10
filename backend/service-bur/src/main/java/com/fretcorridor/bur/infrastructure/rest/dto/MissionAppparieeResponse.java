package com.fretcorridor.bur.infrastructure.rest.dto;

import com.fretcorridor.bur.domain.MissionAppariee;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MissionAppparieeResponse(
        UUID missionId,
        UUID axeId,
        UUID transporteurId,
        String origineNom,
        String destinationNom,
        BigDecimal prixTransport,
        String devise,
        Instant confirmeeLe
) {
    public static MissionAppparieeResponse from(MissionAppariee mission) {
        return new MissionAppparieeResponse(
                mission.missionId(),
                mission.axeId(),
                mission.transporteurId(),
                mission.origineNom(),
                mission.destinationNom(),
                mission.prixTransport(),
                mission.devise(),
                mission.confirmeeLe()
        );
    }
}
