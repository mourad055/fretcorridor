package com.fretcorridor.bur.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record DefinirEstimationMarcheRequest(
        @NotBlank(message = "tenantId est obligatoire") String tenantId,
        @NotNull(message = "axeId est obligatoire") UUID axeId,
        @NotNull(message = "volumeMensuelEstime est obligatoire")
        @Positive(message = "volumeMensuelEstime doit être strictement positif") BigDecimal volumeMensuelEstime,
        @NotBlank(message = "source est obligatoire") String source,
        @NotBlank(message = "acteurId est obligatoire") String acteurId
) {
}
