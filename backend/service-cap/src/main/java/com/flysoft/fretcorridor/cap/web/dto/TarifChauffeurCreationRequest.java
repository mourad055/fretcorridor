package com.flysoft.fretcorridor.cap.web.dto;

import com.flysoft.fretcorridor.cap.domain.CategorieMarchandise;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record TarifChauffeurCreationRequest(
        @NotNull UUID axeId,
        @NotNull CategorieMarchandise categorieMarchandise,
        @NotNull @DecimalMin(value = "0.01", message = "le tarif doit etre strictement positif") BigDecimal tarifFcfaParKg
) {
}
