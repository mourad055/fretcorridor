package com.flysoft.fretcorridor.cap.web.dto;

import com.flysoft.fretcorridor.cap.domain.Capacite;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CapaciteResponse(
        UUID id, UUID vehiculeId, UUID axeId, UUID transporteurId, String modeDeclaration,
        BigDecimal poidsKg, BigDecimal poidsTaxableKg, BigDecimal capaciteResiduelleKg,
        boolean expiree, boolean publiee, Instant dateDepart, Instant dateCreation
) {
    public static CapaciteResponse from(Capacite c) {
        return new CapaciteResponse(c.getId(), c.getVehiculeId(), c.getAxeId(), c.getTransporteurId(),
                c.getModeDeclaration().name(), c.getPoidsKg(), c.getPoidsTaxableKg(),
                c.getCapaciteResiduelleKg(), c.isExpiree(), c.isPubliee(),
                c.getDateDepart(), c.getDateCreation());
    }
}
