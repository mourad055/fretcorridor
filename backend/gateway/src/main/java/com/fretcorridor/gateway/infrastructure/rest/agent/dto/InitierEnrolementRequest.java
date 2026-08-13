package com.fretcorridor.gateway.infrastructure.rest.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InitierEnrolementRequest(@NotBlank String telephone, @NotBlank String typeActeur,
                                        @NotNull Double latitude, @NotNull Double longitude,
                                        @NotBlank String idempotencyKey) {
}
