package com.fretcorridor.geo.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

/**
 * Requete de renseignement du niveau de risque securitaire d'un axe (G3,
 * EF-GEO-04). Toujours filtree/validee avant d'atteindre le domaine (guide
 * de securite - filtrage systematique des inputs).
 */
public record RisqueSecuritaireRequest(

        @NotNull UUID acteurId,

        @NotBlank
        @Pattern(regexp = "NORMAL|SURVEILLE|GELE",
                message = "niveauRisque doit valoir NORMAL, SURVEILLE ou GELE")
        String niveauRisque,

        @NotBlank
        String motif
) {
}
