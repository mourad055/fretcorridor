package com.fretcorridor.gateway.domain.opt;

import java.math.BigDecimal;

public record EtatAlerteVue(
        AlerteSeuilVue alerte,
        boolean evaluable,
        boolean declenchee,
        BigDecimal valeurActuelle
) {
}
