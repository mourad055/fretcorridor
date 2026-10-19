package com.flysoft.fretcorridor.cap.web.dto;

import com.flysoft.fretcorridor.cap.domain.TarifChauffeur;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TarifChauffeurResponse(
        UUID id, UUID axeId, String categorieMarchandise, BigDecimal tarifFcfaParKg,
        boolean actif, Instant dateCreation, Instant dateMiseAJour
) {
    public static TarifChauffeurResponse from(TarifChauffeur t) {
        return new TarifChauffeurResponse(t.getId(), t.getAxeId(), t.getCategorieMarchandise().name(),
                t.getTarifFcfaParKg(), t.isActif(), t.getDateCreation(), t.getDateMiseAJour());
    }
}
