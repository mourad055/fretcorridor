package com.fretcorridor.gateway.infrastructure.rest.ida.dto;

import jakarta.validation.constraints.NotBlank;

public record ModifierTelephoneRequest(@NotBlank String ancienTelephone, @NotBlank String nouveauTelephone) {
}
