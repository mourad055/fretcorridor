package com.fretcorridor.geo.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

public record AxeCreationRequest(

        @NotBlank(message = "le nom de l'axe est obligatoire")
        @Size(max = 150, message = "le nom de l'axe ne doit pas depasser 150 caracteres")
        String nom,

        @NotNull(message = "le hub d'origine est obligatoire")
        UUID hubOrigineId,

        @NotNull(message = "le hub de destination est obligatoire")
        UUID hubDestinationId,

        Boolean visibiliteActive,
        Boolean matchingActif,
        Boolean paiementActif,

        Map<String, Object> parametres,

        // ENF-MUL-01 : optionnel a la creation - null accepte pour compatibilite
        // avec l'existant, mais AxeController applique le tenant BGFT par
        // defaut (meme identifiant que la migration V8) si absent.
        String tenantId
) {
}
