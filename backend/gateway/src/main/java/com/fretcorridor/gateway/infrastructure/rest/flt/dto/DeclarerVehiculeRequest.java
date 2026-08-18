package com.fretcorridor.gateway.infrastructure.rest.flt.dto;

import jakarta.validation.constraints.NotBlank;

public record DeclarerVehiculeRequest(
        @NotBlank String typeVehicule,
        String immatriculation,
        Double profilHauteurMetres,
        Double profilLargeurMetres,
        Double profilLongueurMetres,
        Double profilPoidsMaxTonnes,
        Double profilChargeMaxParEssieuTonnes,
        Integer profilNombreEssieux,
        boolean profilMatieresDangereuses
) {
}
