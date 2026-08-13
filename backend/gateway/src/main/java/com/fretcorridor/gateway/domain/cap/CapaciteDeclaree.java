package com.fretcorridor.gateway.domain.cap;

import java.math.BigDecimal;
import java.time.Instant;

public record CapaciteDeclaree(
        String id, String vehiculeId, String axeId, String modeDeclaration,
        BigDecimal poidsKg, BigDecimal poidsTaxableKg, BigDecimal capaciteResiduelleKg,
        boolean expiree, boolean publiee, Instant dateDepart, Instant dateCreation
) {
}
