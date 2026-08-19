package com.fretcorridor.bur.infrastructure.rest.dto;

import com.fretcorridor.bur.domain.AlerteSeuil;

import java.math.BigDecimal;
import java.time.Instant;

public record AlerteSeuilResponse(
        String id,
        String axeId,
        String indicateur,
        String comparateur,
        BigDecimal seuil,
        String creeParActeurId,
        Instant creeLe
) {
    public static AlerteSeuilResponse from(AlerteSeuil alerte) {
        return new AlerteSeuilResponse(alerte.id(), alerte.axeId().toString(), alerte.indicateur().name(),
                alerte.comparateur().name(), alerte.seuil(), alerte.creeParActeurId(), alerte.creeLe());
    }
}
