package com.fretcorridor.gateway.infrastructure.rest.cap.dto;

import com.fretcorridor.gateway.domain.cap.IntervalleTarif;

import java.math.BigDecimal;

public record IntervalleTarifResponse(BigDecimal tarifMinFcfaParKg, BigDecimal tarifMaxFcfaParKg, int nombreChauffeurs) {
    public static IntervalleTarifResponse from(IntervalleTarif i) {
        return new IntervalleTarifResponse(i.tarifMinFcfaParKg(), i.tarifMaxFcfaParKg(), i.nombreChauffeurs());
    }
}
