package com.fretcorridor.gateway.infrastructure.rest.ida.dto;

import jakarta.validation.constraints.NotBlank;

// numeroRegistreCommerce optionnel au niveau 1 (RG-011) — requis seulement au niveau 2.
public record CompleterEntrepriseRequest(@NotBlank String raisonSociale, String numeroRegistreCommerce) {
}
