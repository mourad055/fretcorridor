package com.fretcorridor.gateway.infrastructure.rest.ida.dto;

import jakarta.validation.constraints.NotBlank;

public record CompleterParticulierRequest(@NotBlank String nom, @NotBlank String prenom) {
}
