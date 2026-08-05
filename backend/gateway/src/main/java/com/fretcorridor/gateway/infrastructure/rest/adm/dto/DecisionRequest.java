package com.fretcorridor.gateway.infrastructure.rest.adm.dto;

import jakarta.validation.constraints.NotBlank;

public record DecisionRequest(
        @NotBlank(message = "La décision est obligatoire") String decision,
        @NotBlank(message = "Le motif est obligatoire") String motif
) {
}
