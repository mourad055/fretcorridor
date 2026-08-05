package com.fretcorridor.adm.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record PriseEnChargeRequest(@NotBlank(message = "L'acteur est obligatoire") String acteurId) {
}
