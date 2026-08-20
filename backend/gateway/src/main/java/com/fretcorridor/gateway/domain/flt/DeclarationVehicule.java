package com.fretcorridor.gateway.domain.flt;

public record DeclarationVehicule(String typeVehicule, String immatriculation, Double profilHauteurMetres,
                                   Double profilLargeurMetres, Double profilLongueurMetres, Double profilPoidsMaxTonnes,
                                   Double profilChargeMaxParEssieuTonnes, Integer profilNombreEssieux,
                                   boolean profilMatieresDangereuses) {
}
