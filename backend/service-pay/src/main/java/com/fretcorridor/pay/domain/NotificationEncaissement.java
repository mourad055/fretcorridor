package com.fretcorridor.pay.domain;

import java.math.BigDecimal;

/** EF-PAY-05 : contenu métier d'une notification d'encaissement entrante du prestataire. */
public record NotificationEncaissement(
        String tenantId,
        String missionId,
        BigDecimal montant,
        String referencePrestataire
) {
}
