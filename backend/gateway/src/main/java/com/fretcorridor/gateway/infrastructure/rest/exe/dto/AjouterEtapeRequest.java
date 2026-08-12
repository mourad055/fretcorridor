package com.fretcorridor.gateway.infrastructure.rest.exe.dto;

import jakarta.validation.constraints.NotBlank;

public record AjouterEtapeRequest(@NotBlank String type, @NotBlank String libelle, String horodatageCapture) {
}
