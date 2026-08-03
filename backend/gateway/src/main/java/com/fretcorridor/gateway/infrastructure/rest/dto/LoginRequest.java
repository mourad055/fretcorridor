package com.fretcorridor.gateway.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Le numéro de téléphone est obligatoire") String phone,
        @NotBlank(message = "Le code est obligatoire") String code
) {
}
