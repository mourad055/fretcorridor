package com.fretcorridor.gateway.infrastructure.rest.opt.dto;

import com.fretcorridor.gateway.domain.opt.MissionAppariee;

import java.time.Instant;

public record MissionAppparieeResponse(
        String id,
        String transporteurNom,
        String origine,
        String destination,
        Instant enlevementLe,
        String statut
) {
    public static MissionAppparieeResponse from(MissionAppariee mission) {
        return new MissionAppparieeResponse(
                mission.id(),
                mission.transporteurNom(),
                mission.origine(),
                mission.destination(),
                mission.enlevementLe(),
                mission.statut().name()
        );
    }
}
