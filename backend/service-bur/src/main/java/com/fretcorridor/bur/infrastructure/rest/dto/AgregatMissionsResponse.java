package com.fretcorridor.bur.infrastructure.rest.dto;

import com.fretcorridor.bur.domain.AgregatMissions;

/** {@code nombreMissions} est null quand le seuil d'agrégation n'est pas atteint (EF-BUR-04). */
public record AgregatMissionsResponse(String axeId, Long nombreMissions, boolean seuilAtteint) {
    public static AgregatMissionsResponse from(AgregatMissions agregat) {
        return new AgregatMissionsResponse(
                agregat.axeId(),
                agregat.nombreMissions().orElse(null),
                agregat.seuilAtteint()
        );
    }
}
