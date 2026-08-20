package com.fretcorridor.pay.domain;

import java.math.BigDecimal;

/** Ligne de relevé restituée par le prestataire de paiement pour une mission. */
public record ReleveLigne(String missionId, BigDecimal montantTotal) {
}
