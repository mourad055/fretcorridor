package com.fretcorridor.gateway.infrastructure.rest.adm.dto;

import jakarta.validation.constraints.NotBlank;

public record DefinirConfigurationRequest(String perimetre, @NotBlank(message = "La valeur est obligatoire") String valeur) {
}
