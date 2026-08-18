package com.fretcorridor.pay.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record DeclarerPaiementEspecesRequest(
        @NotBlank String tenantId,
        @NotNull @Positive BigDecimal montant
) {
}
