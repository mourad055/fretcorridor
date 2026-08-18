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
        String modePaiement,
        BigDecimal montant,
        Instant creeLe,
        String statut,
        boolean litigeActif
) {
    public static EcritureResponse from(EcritureMiroir e, boolean litigeActif) {
        return new EcritureResponse(e.id(), e.missionId(), e.typeCompte().name(), e.nature().name(), e.sens().name(),
                e.modePaiement() == null ? null : e.modePaiement().name(), e.montant(), e.creeLe(), e.statut().name(), litigeActif);
    }
}
