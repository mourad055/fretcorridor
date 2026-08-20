package com.fretcorridor.geo.web.dto;

import com.fretcorridor.geo.domain.TypeHub;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO d'entree pour la creation d'un hub.
 *
 * Toute contrainte est validee ici, AVANT d'atteindre le domaine (Hub) ou la base :
 * c'est la premiere ligne de defense contre les donnees invalides ou malicieuses
 * (cf guide de securite - filtrage/validation systematique de tout input).
 * @Valid sur le controller declenche ces annotations automatiquement.
 */
public record HubCreationRequest(

        // Taille bornee : evite les chaines vides ET les chaines demesurees (deni de service applicatif)
        @NotNull @Size(min = 2, max = 150)
        String nom,

        @NotNull @Size(min = 2, max = 150)
        String ville,

        // NotNull suffit ici : Jackson refuse deja toute valeur qui ne correspond pas
        // exactement a une constante de l'enum TypeHub (VILLE/PLATEFORME/POINT_CONSOLIDATION)
        @NotNull
        TypeHub typeHub,

        // Bornes geographiques reelles (latitude terrestre valide) : rejette toute coordonnee
        // aberrante avant meme de tenter de construire un Point JTS
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
        Double latitude,

        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
        Double longitude,

        // EF-GEO-05 (Phase 4) : code pays ISO 3166-1 alpha-3, obligatoire pour
        // tout nouveau hub - necessaire pour determiner la convention
        // bilaterale applicable (RG-052) si l'axe qui l'utilise est transfrontalier.
        @NotNull @Pattern(regexp = "[A-Z]{3}", message = "pays doit etre un code ISO 3166-1 alpha-3 (ex. CMR, TCD, CAF)")
        String pays
) {}
