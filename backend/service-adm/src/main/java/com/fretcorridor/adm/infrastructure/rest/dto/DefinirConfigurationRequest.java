package com.fretcorridor.adm.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record DefinirConfigurationRequest(
        String perimetre,
        @NotBlank(message = "La valeur est obligatoire") String valeur,
        @NotBlank(message = "L'auteur est obligatoire") String auteur
) {
}
