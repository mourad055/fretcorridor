package com.fretcorridor.gateway.infrastructure.rest.cap.dto;

import com.fretcorridor.gateway.domain.cap.TarifChauffeur;

import java.math.BigDecimal;
import java.time.Instant;

public record TarifChauffeurResponse(
        String id, String axeId, String categorieMarchandise, BigDecimal tarifFcfaParKg,
        boolean actif, Instant dateCreation, Instant dateMiseAJour
) {
    public static TarifChauffeurResponse from(TarifChauffeur t) {
        return new TarifChauffeurResponse(t.id(), t.axeId(), t.categorieMarchandise(), t.tarifFcfaParKg(),
                t.actif(), t.dateCreation(), t.dateMiseAJour());
    }
}
