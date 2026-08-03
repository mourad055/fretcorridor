package com.fretcorridor.geo.web.dto;

import com.fretcorridor.geo.domain.TypeHub;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO d'entree pour la creation d'un hub.
 * Toute contrainte est validee ici avant d'atteindre le domaine (anti-injection, donnees coherentes).
 */
public record HubCreationRequest(

        @NotNull @Size(min = 2, max = 150)
        String nom,

        @NotNull @Size(min = 2, max = 150)
        String ville,

        @NotNull
        TypeHub typeHub,

        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
        Double latitude,

        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
        Double longitude
) {}
