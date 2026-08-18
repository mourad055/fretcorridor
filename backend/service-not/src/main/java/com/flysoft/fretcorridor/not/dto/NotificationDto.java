package com.flysoft.fretcorridor.not.dto;

import com.flysoft.fretcorridor.not.entity.Notification;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

public class NotificationDto {

    @Data
    public static class EnregistrerTokenRequest {
        @NotBlank private String token;
    }

    @Data
    public static class RepondreRequest {
        private boolean accepte;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NotificationResponse {
        private UUID id;
        private String titre;
        private String corps;
        private String type;
        private UUID referenceId;
        private Boolean lue;
        private LocalDateTime dateCreation;
        private Boolean reponseAcceptee;

        public static NotificationResponse fromEntity(Notification n) {
            return NotificationResponse.builder()
                    .id(n.getId())
                    .titre(n.getTitre())
                    .corps(n.getCorps())
                    .type(n.getType().name())
                    .referenceId(n.getReferenceId())
                    .lue(n.getLue())
                    .dateCreation(n.getDateCreation())
                    .reponseAcceptee(n.getReponseAcceptee())
                    .build();
        }
    }
}
