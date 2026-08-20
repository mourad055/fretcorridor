package com.fretcorridor.gateway.infrastructure.rest.flt.dto;

import com.fretcorridor.gateway.domain.flt.Vehicule;

public record VehiculeResponse(String id, String typeVehicule, String immatriculation, Double profilHauteurMetres,
                                Double profilLargeurMetres, Double profilLongueurMetres, Double profilPoidsMaxTonnes,
                                Double profilChargeMaxParEssieuTonnes, Integer profilNombreEssieux,
                                boolean profilMatieresDangereuses, String dateCreation) {
    public static VehiculeResponse from(Vehicule v) {
        return new VehiculeResponse(v.id(), v.typeVehicule(), v.immatriculation(), v.profilHauteurMetres(),
                v.profilLargeurMetres(), v.profilLongueurMetres(), v.profilPoidsMaxTonnes(),
                v.profilChargeMaxParEssieuTonnes(), v.profilNombreEssieux(), v.profilMatieresDangereuses(), v.dateCreation());
    }
}
