package com.fretcorridor.pay.infrastructure.rest.dto;

import com.fretcorridor.pay.domain.Garantie;

import java.math.BigDecimal;
import java.time.Instant;

public record GarantieResponse(
        String id,
        String missionId,
        String garantId,
        BigDecimal montant,
        String referenceGarantie,
        Instant engageeLe
) {
    public static GarantieResponse from(Garantie g) {
        return new GarantieResponse(g.id(), g.missionId(), g.garantId(), g.montant(), g.referenceGarantie(), g.engageeLe());
    }
}
