package com.fretcorridor.gateway.infrastructure.rest.cap.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

/** Miroir de CapaciteCreationRequest (service-cap), mêmes contraintes. */
public record DeclarerCapaciteRequest(
        @NotBlank String vehiculeId,
        @NotBlank String axeId,
        @NotBlank String modeDeclaration,

        @NotNull @Positive BigDecimal poidsKg,
        @Positive BigDecimal volumeM3,
        @Positive BigDecimal longueurPlancherM,

        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double origineLatitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double origineLongitude,

        @NotBlank String typeVehicule,

        Double profilHauteurMetres,
        Double profilLargeurMetres,
        Double profilLongueurMetres,
        Double profilPoidsMaxTonnes,
        Double profilChargeMaxParEssieuTonnes,
        Integer profilNombreEssieux,
        boolean profilMatieresDangereuses,

        @NotNull @Future(message = "la date de départ doit être dans le futur") Instant dateDepart
) {
}
