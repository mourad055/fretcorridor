package com.fretcorridor.gateway.infrastructure.rest.opt.dto;

import com.fretcorridor.gateway.domain.opt.EtatAlerteVue;

import java.math.BigDecimal;

public record EtatAlerteResponse(
        AlerteSeuilResponse alerte,
        boolean evaluable,
        boolean declenchee,
        BigDecimal valeurActuelle
) {
    public static EtatAlerteResponse from(EtatAlerteVue vue) {
        return new EtatAlerteResponse(AlerteSeuilResponse.from(vue.alerte()), vue.evaluable(), vue.declenchee(),
                vue.valeurActuelle());
    }
}
