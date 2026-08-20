package com.fretcorridor.pay.infrastructure.rest.dto;

import com.fretcorridor.pay.domain.ModePaiement;
import jakarta.validation.constraints.NotNull;

public record ChoisirModePaiementRequest(
        @NotNull ModePaiement modePaiement
) {
}
