package com.fretcorridor.geo.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

/**
 * Requete de renseignement de la cle de repartition conventionnelle d'un
 * axe transfrontalier (RG-052, G4). Toujours filtree/validee avant
 * d'atteindre le domaine (guide de securite).
 */
public record ConventionRepartitionRequest(

        @NotNull UUID acteurId,

        @NotBlank String conventionCode,

        // Cle = code pays ISO 3166-1 alpha-3, valeur = part en pourcentage.
        // Somme = 100 verifiee cote service, jamais supposee cote client.
        @NotEmpty Map<String, Double> partsPourcent,

        @NotBlank String motif
) {
}
