package com.fretcorridor.gateway.domain.cap;

import java.math.BigDecimal;
import java.time.Instant;

/** Miroir de TarifChauffeurResponse (service-cap), plan §1bis. */
public record TarifChauffeur(
        String id, String axeId, String categorieMarchandise, BigDecimal tarifFcfaParKg,
        boolean actif, Instant dateCreation, Instant dateMiseAJour) {
}
