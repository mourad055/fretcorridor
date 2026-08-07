package com.flysoft.fretcorridor.ida.dto;

import com.flysoft.fretcorridor.ida.entity.Acteur;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.util.List;
import java.util.UUID;

public class AuthDto {

    @Data
    public static class LoginRequest {
        @NotBlank private String telephone;
        @NotBlank private String codePin;
    }

    // EF-MKT-01 : inscription légère chargeur, sans KYC bloquant à ce stade (S1)
    @Data
    public static class InscriptionChargeurRequest {
        @NotBlank private String telephone;
        @NotBlank private String codePin;
        private String nom;           // optionnel si personne morale
        private String prenom;
        private String raisonSociale; // optionnel si personne physique
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private UUID acteurId;
        private List<String> roles;
        private String tenantId;

        public static AuthResponse of(String access, String refresh, Acteur acteur) {
            return AuthResponse.builder()
                    .accessToken(access)
                    .refreshToken(refresh)
                    .acteurId(acteur.getId())
                    .roles(acteur.getRoles().stream().map(Enum::name).toList())
                    .tenantId(acteur.getTenantId())
                    .build();
        }
    }
}
