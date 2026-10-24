package com.fretcorridor.gateway.infrastructure.rest.cap.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ModifierTarifChauffeurRequest(
        @NotNull @DecimalMin(value = "0.01", message = "le tarif doit être strictement positif") BigDecimal tarifFcfaParKg
) {
}
