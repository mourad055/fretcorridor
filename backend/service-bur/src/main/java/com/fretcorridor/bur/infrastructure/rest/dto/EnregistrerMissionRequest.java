package com.fretcorridor.bur.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record EnregistrerMissionRequest(
        @NotBlank(message = "axeId est obligatoire") String axeId
) {
}
