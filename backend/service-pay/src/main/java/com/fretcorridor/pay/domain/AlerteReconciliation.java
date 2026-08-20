package com.fretcorridor.pay.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record AlerteReconciliation(String missionId, BigDecimal ecart, Instant declencheeLe, boolean bloquante) {
}
