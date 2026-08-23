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

    // Inscription légère chauffeur/transporteur (app Chauffeur/Transporteur,
    // même principe que InscriptionChargeurRequest) - le profil complet (KYC,
    // véhicules) vient après, cet écran ne fait que créer le compte.
    @Data
    public static class InscriptionTransporteurRequest {
        @NotBlank private String telephone;
        @NotBlank private String codePin;
        @NotBlank private String type; // CHAUFFEUR, TRANSPORTEUR ou CHAUFFEUR_PROPRIETAIRE
        private String nom;
        private String prenom;
        private String raisonSociale; // pertinent seulement pour TRANSPORTEUR
    }

    // Modification du numéro de téléphone (compte connecté) - l'ancien numéro
    // doit être confirmé pour éviter qu'un tiers ayant accès à l'appareil
    // déverrouillé ne change silencieusement l'identifiant de connexion du
    // titulaire du compte.
    @Data
    public static class ModifierTelephoneRequest {
        @NotBlank private String ancienTelephone;
        @NotBlank private String nouveauTelephone;
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
            return of(access, refresh, acteur, acteur.getTenantId());
        }

        // S18 : tenantId affiche/renvoye = celui EFFECTIVEMENT porte par le
        // token (cf JwtService.genererAccessToken(Acteur, String)), pas
        // necessairement le tenant d'origine de l'acteur.
        public static AuthResponse of(String access, String refresh, Acteur acteur, String tenantIdEffectif) {
            return AuthResponse.builder()
                    .accessToken(access)
                    .refreshToken(refresh)
                    .acteurId(acteur.getId())
                    .roles(acteur.getRoles().stream().map(Enum::name).toList())
                    .tenantId(tenantIdEffectif)
                    .build();
        }
    }

    // S18 : liste des tenants sous lesquels cet acteur peut operer (tenant
    // d'origine + affiliations accordees par d'autres bureaux).
    public record TenantDisponible(String tenantId, boolean origine) {
    }
}
