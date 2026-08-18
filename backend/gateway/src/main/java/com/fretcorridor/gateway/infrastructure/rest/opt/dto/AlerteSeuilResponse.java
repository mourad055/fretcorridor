package com.fretcorridor.gateway.infrastructure.rest.opt.dto;

import com.fretcorridor.gateway.domain.opt.AlerteSeuilVue;

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
    public static AlerteSeuilResponse from(AlerteSeuilVue vue) {
        return new AlerteSeuilResponse(vue.id(), vue.axeId(), vue.indicateur(), vue.comparateur(), vue.seuil(),
                vue.creeParActeurId(), vue.creeLe());
    }
}
