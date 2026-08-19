package com.fretcorridor.pay.infrastructure.rest.dto;

import com.fretcorridor.pay.domain.ModePaiement;
import com.fretcorridor.pay.domain.ModePaiementChoisi;

import java.time.Instant;

public record ModePaiementChoisiResponse(
        String missionId,
        ModePaiement modePaiement,
        Instant choisiLe
) {
    public static ModePaiementChoisiResponse from(ModePaiementChoisi choix) {
        return new ModePaiementChoisiResponse(choix.missionId(), choix.modePaiement(), choix.choisiLe());
    }
}
