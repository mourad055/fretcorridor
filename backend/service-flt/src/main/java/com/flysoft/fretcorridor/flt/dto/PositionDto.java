package com.flysoft.fretcorridor.flt.dto;

import com.flysoft.fretcorridor.flt.entity.Position;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

public class PositionDto {

    @Data
    public static class EnvoyerRequest {
        @NotNull private UUID missionId;
        @NotNull private Double latitude;
        @NotNull private Double longitude;
        @NotNull private LocalDateTime horodatage;

        // FIX audit 21/08 (position-brute.yaml:38-45, ENF-SEC-03) : clé
        // d'idempotence générée par l'app AU MOMENT DE LA CAPTURE et
        // préservée à travers flt -> Kafka -> trk, pour que la déduplication
        // trk (UNIQUE(event_id)) reconnaisse les ré-envois hors ligne.
        // Optionnel pour compatibilité : absent => généré à la publication
        // (comportement historique, sans garantie anti-doublon).
        private UUID eventId;

        // FIX bloquant audit 21/08 : "MOBILE_CHAUFFEUR" était rejeté par la
        // contrainte chk_source_capture de service-trk (V2, enum contrat
        // position-brute.yaml:58-60) - chaque position finissait loguée
        // "doublon ignoré", pipeline temps réel mort. Valeurs acceptées :
        // GPS_NATIF | GPS_DEGRADE | MANUEL. Absent => GPS_NATIF.
        @Pattern(regexp = "GPS_NATIF|GPS_DEGRADE|MANUEL", message = "sourceCapture doit être GPS_NATIF, GPS_DEGRADE ou MANUEL")
        private String sourceCapture;

        // Contrat position-brute.yaml:62-66 : précision GPS rapportée par
        // l'appareil, si disponible (EF-TRK-04).
        @DecimalMin("0.0")
        private Double precisionMetres;
    }

    // EF-TRK-04 : toute position affichée avec son horodatage ET son âge
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DernierePositionResponse {
        private Double latitude;
        private Double longitude;
        private LocalDateTime horodatage;
        private Long ageSecondes;

        public static DernierePositionResponse fromEntity(Position p) {
            return DernierePositionResponse.builder()
                    .latitude(p.getLatitude())
                    .longitude(p.getLongitude())
                    .horodatage(p.getHorodatage())
                    .ageSecondes(Duration.between(p.getHorodatage(), LocalDateTime.now()).getSeconds())
                    .build();
        }
    }
}
