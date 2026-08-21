package com.fretcorridor.gateway.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank(message = "Le numéro de téléphone est obligatoire") String phone,
        @NotBlank(message = "Le code est obligatoire") String code,
        @NotBlank(message = "Le type de compte est obligatoire") String type,
        String nom,
        String prenom,
        String raisonSociale
) {
}
