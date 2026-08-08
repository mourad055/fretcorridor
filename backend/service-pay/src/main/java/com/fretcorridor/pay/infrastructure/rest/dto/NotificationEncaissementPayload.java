package com.fretcorridor.pay.infrastructure.rest.dto;

import com.fretcorridor.pay.domain.NotificationEncaissement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** Corps JSON attendu du webhook prestataire (EF-PAY-05). */
public record NotificationEncaissementPayload(
        @NotBlank String tenantId,
        @NotBlank String missionId,
        @NotNull @Positive BigDecimal montant,
        @NotBlank String referencePrestataire
) {
    public NotificationEncaissement versDomaine() {
        return new NotificationEncaissement(tenantId, missionId, montant, referencePrestataire);
    }
}
