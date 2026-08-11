package com.fretcorridor.pay.infrastructure.rest.dto;

import com.fretcorridor.pay.domain.ModePaiement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ClotureMissionRequest(
        @NotBlank String tenantId,
        @NotBlank String transporteurId,
        @NotNull @Positive BigDecimal montant,
        @NotBlank String referencePrestataire,
        @NotNull ModePaiement modePaiement
) {
}
