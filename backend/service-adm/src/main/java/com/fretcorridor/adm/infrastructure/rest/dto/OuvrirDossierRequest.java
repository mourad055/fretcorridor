package com.fretcorridor.adm.infrastructure.rest.dto;

import com.fretcorridor.adm.domain.PrioriteDossier;
import com.fretcorridor.adm.domain.TypeDossier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record OuvrirDossierRequest(
        @NotBlank(message = "Le tenant est obligatoire") String tenantId,
        @NotNull(message = "Le type est obligatoire") TypeDossier type,
        @NotNull(message = "La priorité est obligatoire") PrioriteDossier priorite,
        String missionId,
        List<String> parties,
        List<String> preuvesReferences,
        @NotNull(message = "Le délai de traitement est obligatoire") Instant delaiTraitement
) {
}
