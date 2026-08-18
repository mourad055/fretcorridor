package com.fretcorridor.gateway.infrastructure.rest.agent.dto;

import jakarta.validation.constraints.NotBlank;

public record ActiverEnrolementRequest(@NotBlank String otp, @NotBlank String codePin) {
}
