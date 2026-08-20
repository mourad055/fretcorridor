package com.fretcorridor.adm.infrastructure.messaging;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Miroir exact de l'événement publié par service-pay
 * (com.fretcorridor.pay.infrastructure.messaging.EcartReconciliationEvent) —
 * ouvre un Dossier de type INCIDENT dans la file de travail (EF-PAY-09).
 */
public record EcartReconciliationEvent(
        String eventId,
        String missionId,
        String tenantId,
        BigDecimal ecart,
        Instant declencheeLe
) {
}
