package com.flysoft.fretcorridor.flt.dto;

import com.flysoft.fretcorridor.flt.entity.Position;
import jakarta.validation.constraints.NotNull;
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
