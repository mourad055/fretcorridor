package com.fretcorridor.gateway.infrastructure.rest.pay.dto;

import com.fretcorridor.gateway.domain.pay.ModePaiementChoisi;
import java.time.Instant;

public record ModePaiementChoisiResponse(String missionId, String modePaiement, Instant choisiLe) {
    public static ModePaiementChoisiResponse from(ModePaiementChoisi m) {
        return new ModePaiementChoisiResponse(m.missionId(), m.modePaiement(), m.choisiLe());
    }
}
