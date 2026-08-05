package com.fretcorridor.adm.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record DecisionRequest(
        @NotBlank(message = "La décision est obligatoire") String decision,
        @NotBlank(message = "Le motif est obligatoire") String motif,
        @NotBlank(message = "L'acteur est obligatoire") String acteurId
) {
}
