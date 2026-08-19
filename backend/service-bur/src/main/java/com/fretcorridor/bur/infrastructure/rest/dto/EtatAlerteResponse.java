package com.fretcorridor.bur.infrastructure.rest.dto;

import com.fretcorridor.bur.domain.EtatAlerte;

import java.math.BigDecimal;

public record EtatAlerteResponse(
        AlerteSeuilResponse alerte,
        boolean evaluable,
        boolean declenchee,
        BigDecimal valeurActuelle
) {
    public static EtatAlerteResponse from(EtatAlerte etat) {
        return new EtatAlerteResponse(AlerteSeuilResponse.from(etat.alerte()), etat.evaluable(), etat.declenchee(),
                etat.valeurActuelle());
    }
}
