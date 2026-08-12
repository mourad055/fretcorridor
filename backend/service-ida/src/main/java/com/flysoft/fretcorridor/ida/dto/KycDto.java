package com.flysoft.fretcorridor.ida.dto;

import com.flysoft.fretcorridor.ida.entity.Acteur;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class KycDto {

    @Data
    public static class CompleterProfilParticulierRequest {
        @NotBlank private String nom;
        @NotBlank private String prenom;
    }

    @Data
    public static class CompleterProfilEntrepriseRequest {
        @NotBlank private String raisonSociale;
        private String numeroRegistreCommerce; // optionnel au niveau 1 (RG-011)
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProfilResponse {
        private UUID acteurId;
        private String type; // PARTICULIER ou ENTREPRISE
        private String nom;
        private String prenom;
        private String raisonSociale;
        private String niveauKyc;
        @Builder.Default
        private List<PieceResponse> pieces = List.of();

        public static ProfilResponse fromEntity(Acteur a, List<PieceResponse> pieces) {
            boolean estEntreprise = a.getOrganisation() != null;
            return ProfilResponse.builder()
                    .acteurId(a.getId())
                    .type(estEntreprise ? "ENTREPRISE" : "PARTICULIER")
                    .nom(a.getNom())
                    .prenom(a.getPrenom())
                    .raisonSociale(estEntreprise ? a.getOrganisation().getRaisonSociale() : null)
                    .niveauKyc(a.getNiveauKyc().name())
                    .pieces(pieces)
                    .build();
        }
    }

    // ── Pièce justificative déposée (EF-IDA-03) ───────────────
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PieceResponse {
        private String typeDocument;
        private String url; // présignée, à durée limitée — jamais persistée telle quelle
        private LocalDateTime dateDepot;
    }

    // Renvoyé après complétion — nouveaux tokens avec le niveauKyc à jour
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CompletionResponse {
        private String accessToken;
        private String refreshToken;
        private ProfilResponse profil;
    }
}
