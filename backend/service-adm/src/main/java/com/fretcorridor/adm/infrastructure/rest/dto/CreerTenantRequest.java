package com.fretcorridor.adm.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record CreerTenantRequest(
        @NotBlank(message = "L'identifiant est obligatoire") String id,
        @NotBlank(message = "Le nom est obligatoire") String nom,
        @NotBlank(message = "Le pays est obligatoire") String pays
) {
}
