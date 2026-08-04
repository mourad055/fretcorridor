package com.fretcorridor.mat.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

/**
 * Un candidat (capacite) evalue pour une demande donnee, avec ses valeurs de
 * critere deja normalisees par l'appelant (OPT) - cf limitation documentee
 * dans CoutCompositeService.
 *
 * @Size(max = 10) sur la Map : garde-fou anti-abus (pas de limite fonctionnelle
 * connue qui justifierait plus de 10 criteres en V0), pas juste une contrainte
 * de forme - conforme au reflexe "filtrer/valider tous les inputs".
 */
public record PaireCandidatRequest(

        @NotNull
        UUID capaciteId,

        @NotNull
        @NotEmpty
        @Size(max = 10, message = "10 criteres maximum par candidat")
        Map<String, Double> valeursCriteres
) {
}
