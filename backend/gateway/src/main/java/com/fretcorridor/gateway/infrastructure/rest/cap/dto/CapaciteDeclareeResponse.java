package com.fretcorridor.gateway.infrastructure.rest.cap.dto;

import com.fretcorridor.gateway.domain.cap.CapaciteDeclaree;

import java.math.BigDecimal;
import java.time.Instant;

public record CapaciteDeclareeResponse(
        String id, String vehiculeId, String axeId, String modeDeclaration,
        BigDecimal poidsKg, BigDecimal poidsTaxableKg, BigDecimal capaciteResiduelleKg,
        boolean expiree, boolean publiee, Instant dateDepart, Instant dateCreation
) {
    public static CapaciteDeclareeResponse from(CapaciteDeclaree c) {
        return new CapaciteDeclareeResponse(c.id(), c.vehiculeId(), c.axeId(), c.modeDeclaration(),
                c.poidsKg(), c.poidsTaxableKg(), c.capaciteResiduelleKg(),
                c.expiree(), c.publiee(), c.dateDepart(), c.dateCreation());
    }
}
