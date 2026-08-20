package com.flysoft.fretcorridor.ida.dto;

import com.flysoft.fretcorridor.ida.entity.EnrolementAgent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import java.util.UUID;

public class EnrolementDto {

    // ── Initier un enrôlement (par l'agent) ───────────────────
    @Data
    public static class InitierRequest {
        @NotBlank
        @Pattern(regexp = "^\\+?[0-9]{9,15}$")
        private String telephone;

        @NotBlank
        private String typeActeur; // CHAUFFEUR, TRANSPORTEUR ou CHAUFFEUR_PROPRIETAIRE

        @NotNull private Double latitude;
        @NotNull private Double longitude;

        // Générée côté client (file offline) pour que rejouer la même requête
        // après une coupure réseau ne crée jamais deux enrôlements (RG-019/A1).
        @NotBlank private String idempotencyKey;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EnrolementResponse {
        private UUID enrolementId;
        private String telephone;
        private String typeActeur;
        private String statut;

        public static EnrolementResponse fromEntity(EnrolementAgent e) {
            return EnrolementResponse.builder()
                    .enrolementId(e.getId())
                    .telephone(e.getTelephoneEnrole())
                    .typeActeur(e.getTypeActeur().name())
                    .statut(e.getStatut().name())
                    .build();
        }
    }

    // ── Activer un enrôlement (code reçu par SMS + PIN choisi par la personne) ─
    @Data
    public static class ActiverRequest {
        @NotBlank private String otp;

        @NotBlank
        @Pattern(regexp = "^[0-9]{4,6}$")
        private String codePin;
    }
}
