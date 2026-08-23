package com.fretcorridor.adm.infrastructure.rest.dto;

import com.fretcorridor.adm.domain.PrioriteDossier;
import com.fretcorridor.adm.domain.TypeDossier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record OuvrirDossierRequest(
        @NotNull(message = "Le type est obligatoire") TypeDossier type,
        @NotNull(message = "La priorité est obligatoire") PrioriteDossier priorite,
        String missionId,
        List<String> parties,
        List<String> preuvesReferences,
        // Motif/description (audit de suivi, 23 aout) - cf javadoc Dossier.
        String motif,
        String description,
        // Nullable depuis le 23 aout (S19, litige initie par un chargeur,
        // Mobile) : cf javadoc DossierController.ouvrir sur le delai par
        // defaut applique quand absent - un auteur ADM interne connait un
        // vrai delai de traitement, un chargeur qui signale un litige n'en
        // a aucune idee.
        Instant delaiTraitement
) {
}
