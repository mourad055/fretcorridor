package com.fretcorridor.bur.infrastructure.rest.dto;

import com.fretcorridor.bur.domain.Comparateur;
import com.fretcorridor.bur.domain.IndicateurObservatoire;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ConfigurerAlerteRequest(
        @NotBlank(message = "tenantId est obligatoire") String tenantId,
        @NotNull(message = "axeId est obligatoire") UUID axeId,
        @NotNull(message = "indicateur est obligatoire") IndicateurObservatoire indicateur,
        @NotNull(message = "comparateur est obligatoire") Comparateur comparateur,
        @NotNull(message = "seuil est obligatoire") BigDecimal seuil,
        @NotBlank(message = "acteurId est obligatoire") String acteurId
) {
}
