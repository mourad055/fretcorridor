package com.fretcorridor.pay.domain;

import java.math.BigDecimal;
import java.time.Instant;

/** EF-PAY-09, ENF-FIN-03 : lève l'alerte bloquante vers la file de travail Admin. */
public interface ReconciliationEventPort {

    void publier(String missionId, String tenantId, BigDecimal ecart, Instant declencheeLe);
}
