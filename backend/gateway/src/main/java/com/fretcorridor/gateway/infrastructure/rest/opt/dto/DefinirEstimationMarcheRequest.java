package com.fretcorridor.gateway.infrastructure.rest.opt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record DefinirEstimationMarcheRequest(
        @NotNull(message = "volumeMensuelEstime est obligatoire")
        @Positive(message = "volumeMensuelEstime doit être strictement positif") BigDecimal volumeMensuelEstime,
        @NotBlank(message = "source est obligatoire") String source
) {
}
