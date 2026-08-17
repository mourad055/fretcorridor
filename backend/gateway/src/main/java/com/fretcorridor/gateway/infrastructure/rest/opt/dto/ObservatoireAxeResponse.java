package com.fretcorridor.gateway.infrastructure.rest.opt.dto;

import com.fretcorridor.gateway.domain.opt.ObservatoireAxeVue;

import java.math.BigDecimal;

public record ObservatoireAxeResponse(
        String axeId,
        long seuil,
        boolean seuilAtteint,
        Long nombreMissions,
        BigDecimal prixMediane,
        BigDecimal prixDispersion,
        String devise,
        Double tauxDesequilibreDirectionnel
) {
    public static ObservatoireAxeResponse from(ObservatoireAxeVue vue) {
        return new ObservatoireAxeResponse(vue.axeId(), vue.seuil(), vue.seuilAtteint(), vue.nombreMissions(),
                vue.prixMediane(), vue.prixDispersion(), vue.devise(), vue.tauxDesequilibreDirectionnel());
    }
}
