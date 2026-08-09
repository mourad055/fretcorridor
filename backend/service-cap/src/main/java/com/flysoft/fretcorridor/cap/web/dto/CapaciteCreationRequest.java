package com.flysoft.fretcorridor.cap.web.dto;

import com.flysoft.fretcorridor.cap.domain.ModeDeclaration;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * EF-CAP-03 : les trois modes de declaration partagent ce meme DTO -
 * volumeM3/longueurPlancherM optionnels selon le mode (TOTALE peut s'en
 * passer, PRECISE les exige en pratique cote validation metier, pas
 * Bean Validation ici pour rester simple en V0).
 */
public record CapaciteCreationRequest(

        @NotNull UUID vehiculeId,
        @NotNull UUID axeId,
        @NotNull ModeDeclaration modeDeclaration,

        @NotNull @Positive
        BigDecimal poidsKg,

        @Positive
        BigDecimal volumeM3,

        @Positive
        BigDecimal longueurPlancherM,

        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
        Double origineLatitude,

        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
        Double origineLongitude,

        @NotBlank
        String typeVehicule,

        Double profilHauteurMetres,
        Double profilLargeurMetres,
        Double profilLongueurMetres,
        Double profilPoidsMaxTonnes,
        Double profilChargeMaxParEssieuTonnes,
        Integer profilNombreEssieux,
        boolean profilMatieresDangereuses,

        @NotNull @Future(message = "la date de depart doit etre dans le futur")
        Instant dateDepart
) {
}
