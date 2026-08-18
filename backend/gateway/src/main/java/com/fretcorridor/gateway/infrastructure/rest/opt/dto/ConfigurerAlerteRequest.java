package com.fretcorridor.gateway.infrastructure.rest.opt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ConfigurerAlerteRequest(
        @NotBlank(message = "axeId est obligatoire") String axeId,
        @NotBlank(message = "indicateur est obligatoire") String indicateur,
        @NotBlank(message = "comparateur est obligatoire") String comparateur,
        @NotNull(message = "seuil est obligatoire") BigDecimal seuil
) {
}
