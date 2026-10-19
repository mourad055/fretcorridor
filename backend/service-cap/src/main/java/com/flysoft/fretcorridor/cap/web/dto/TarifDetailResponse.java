package com.flysoft.fretcorridor.cap.web.dto;

import com.flysoft.fretcorridor.cap.domain.TarifChauffeur;

import java.math.BigDecimal;
import java.util.UUID;

/** Reponse de GET /api/cap/tarifs/detail (lecture interne, OPT). */
public record TarifDetailResponse(UUID transporteurId, UUID axeId, String categorieMarchandise, BigDecimal tarifFcfaParKg) {
    public static TarifDetailResponse from(TarifChauffeur t) {
        return new TarifDetailResponse(t.getTransporteurId(), t.getAxeId(), t.getCategorieMarchandise().name(), t.getTarifFcfaParKg());
    }
}
