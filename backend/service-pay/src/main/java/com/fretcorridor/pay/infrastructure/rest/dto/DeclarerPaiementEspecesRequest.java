package com.fretcorridor.pay.infrastructure.rest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record DeclarerPaiementEspecesRequest(
        @NotNull @Positive BigDecimal montant
) {
}
