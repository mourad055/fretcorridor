package com.flysoft.fretcorridor.cap.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TarifChauffeurModificationRequest(
        @NotNull @DecimalMin(value = "0.01", message = "le tarif doit etre strictement positif") BigDecimal tarifFcfaParKg
) {
}
