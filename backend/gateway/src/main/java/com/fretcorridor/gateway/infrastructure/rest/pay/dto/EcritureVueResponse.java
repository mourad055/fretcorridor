package com.fretcorridor.gateway.infrastructure.rest.pay.dto;

import com.fretcorridor.gateway.domain.pay.EcritureVue;

import java.math.BigDecimal;
import java.time.Instant;

public record EcritureVueResponse(
        String id,
        String missionId,
        String typeCompte,
        String nature,
        String sens,
        BigDecimal montant,
        Instant creeLe,
        String statut,
        String modePaiement,
        boolean litigeActif
) {
    public static EcritureVueResponse from(EcritureVue e) {
        return new EcritureVueResponse(e.id(), e.missionId(), e.typeCompte(), e.nature(), e.sens(), e.montant(), e.creeLe(), e.statut(),
                e.modePaiement(), e.litigeActif());
    }
}
