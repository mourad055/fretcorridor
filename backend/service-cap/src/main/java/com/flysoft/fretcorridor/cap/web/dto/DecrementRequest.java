package com.flysoft.fretcorridor.cap.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** EF-CAP-07 : cle d'idempotence obligatoire, jamais un decrement "nu". */
public record DecrementRequest(
        @NotNull @Positive BigDecimal montantKg,
        @NotBlank String cleIdempotence
) {
}
