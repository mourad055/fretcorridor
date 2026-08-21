package com.fretcorridor.gateway.infrastructure.rest.flt.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record EnvoyerPositionRequest(
        @NotBlank String missionId,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @NotBlank String horodatage,
        // FIX audit 21/08 : clé d'idempotence générée par l'app à la capture
        // (position-brute.yaml:38-45, ENF-SEC-03) - optionnel pour compat.
        @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", message = "eventId doit être un UUID")
        String eventId,
        // FIX bloquant audit 21/08 : enum du contrat position-brute.yaml:58-60
        // ("MOBILE_CHAUFFEUR" était rejeté par la CHECK de service-trk).
        @Pattern(regexp = "GPS_NATIF|GPS_DEGRADE|MANUEL", message = "sourceCapture doit être GPS_NATIF, GPS_DEGRADE ou MANUEL")
        String sourceCapture,
        @DecimalMin("0.0") Double precisionMetres
) {
}
