package com.fretcorridor.pay.infrastructure.rest.dto;

import com.fretcorridor.pay.domain.EcritureMiroir;

import java.math.BigDecimal;
import java.time.Instant;

public record EcritureResponse(
        String id,
        String missionId,
        String typeCompte,
        String nature,
        String sens,
        BigDecimal montant,
        Instant creeLe,
        String statut
) {
    public static EcritureResponse from(EcritureMiroir e) {
        return new EcritureResponse(e.id(), e.missionId(), e.typeCompte().name(), e.nature().name(), e.sens().name(), e.montant(), e.creeLe(), e.statut().name());
    }
}
