package com.fretcorridor.gateway.infrastructure.rest.kyc.dto;

import jakarta.validation.constraints.NotNull;

import com.fretcorridor.gateway.domain.kyc.KycStatut;

public record DecisionRequest(
        @NotNull(message = "La décision est obligatoire") KycStatut decision,
        String motif
) {
    public DecisionRequest(KycStatut decision) {
        this(decision, null);
    }
}
