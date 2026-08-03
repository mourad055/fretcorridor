package com.fretcorridor.geo.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

/**
 * Requete de creation d'un axe.
 * Toutes les contraintes sont verifiees par Bean Validation avant d'atteindre
 * le controleur (@Valid) - aucune donnee non filtree n'atteint la couche domaine.
 */
public record AxeCreationRequest(

        // Nom obligatoire, non vide et non uniquement des espaces (@NotBlank).
        @NotBlank(message = "le nom de l'axe est obligatoire")
        @Size(max = 150, message = "le nom de l'axe ne doit pas depasser 150 caracteres")
        String nom,

        // Reference vers un Hub existant - la verification d'existence reelle
        // se fait cote controleur (404 si l'UUID ne correspond a aucun hub).
        @NotNull(message = "le hub d'origine est obligatoire")
        UUID hubOrigineId,

        @NotNull(message = "le hub de destination est obligatoire")
        UUID hubDestinationId,

        // Optionnels a la creation : un axe nait souvent desactive (EF-GEO-03),
        // puis est active progressivement une fois pret. Si null, le service
        // applique la valeur par defaut (false) definie dans l'entite Axe.
        Boolean visibiliteActive,
        Boolean matchingActif,
        Boolean paiementActif,

        // Parametres de matching/tarification propres a l'axe (EF-GEO-02).
        // Cle libre en JSON pour rester configurable sans migration a chaque
        // nouvelle cle (anti-patron "code en dur" explicitement proscrit au CDC).
        Map<String, Object> parametres
) {
}
