package com.fretcorridor.gateway.domain.flt;

/** S10 : véhicule de la console de flotte simplifiée. */
public record Vehicule(String id, String typeVehicule, String immatriculation, Double profilHauteurMetres,
                        Double profilLargeurMetres, Double profilLongueurMetres, Double profilPoidsMaxTonnes,
                        Double profilChargeMaxParEssieuTonnes, Integer profilNombreEssieux,
                        boolean profilMatieresDangereuses, String dateCreation,
                        boolean photoCarteGriseRectoDeposee, boolean photoCarteGriseVersoDeposee) {
}
