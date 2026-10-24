package com.fretcorridor.gateway.infrastructure.rest.cap.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Miroir de TarifChauffeurCreationRequest (service-cap), mêmes contraintes. */
public record ConfigurerTarifChauffeurRequest(
        @NotBlank String axeId,
        @NotBlank String categorieMarchandise,
        @NotNull @DecimalMin(value = "0.01", message = "le tarif doit être strictement positif") BigDecimal tarifFcfaParKg
) {
}
