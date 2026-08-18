package com.fretcorridor.pay.infrastructure.messaging;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Miroir exact du contrat consommé par service-adm
 * (com.fretcorridor.adm.infrastructure.messaging.EcartReconciliationEvent) —
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
