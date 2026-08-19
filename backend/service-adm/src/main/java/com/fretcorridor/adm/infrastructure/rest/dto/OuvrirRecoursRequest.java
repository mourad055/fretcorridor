package com.fretcorridor.adm.infrastructure.rest.dto;

import com.fretcorridor.adm.domain.PrioriteDossier;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record OuvrirRecoursRequest(
        @NotNull(message = "La priorité est obligatoire") PrioriteDossier priorite,
        @NotNull(message = "Le délai de traitement est obligatoire") Instant delaiTraitement
) {
}
