package com.fretcorridor.gateway.infrastructure.rest.adm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ModifierTenantRequest(
        @NotBlank(message = "Le nom est obligatoire") String nom,
        @NotBlank(message = "Le pays est obligatoire") String pays,
        @NotNull(message = "actif est obligatoire") Boolean actif
) {
}
