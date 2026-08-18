package com.fretcorridor.gateway.infrastructure.rest.pay.dto;

import com.fretcorridor.gateway.domain.pay.DeclarationEspecesVue;

import java.math.BigDecimal;
import java.time.Instant;

public record DeclarationEspecesVueResponse(
        String id,
        String missionId,
        BigDecimal montant,
        Instant declareeLe,
        boolean protectionAssuree
) {
    public static DeclarationEspecesVueResponse from(DeclarationEspecesVue d) {
        return new DeclarationEspecesVueResponse(d.id(), d.missionId(), d.montant(), d.declareeLe(), d.protectionAssuree());
    }
}
