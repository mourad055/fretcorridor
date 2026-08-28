package com.flysoft.fretcorridor.ida.dto;

import com.flysoft.fretcorridor.ida.entity.Acteur;
import com.flysoft.fretcorridor.ida.entity.RoleActeur;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

/**
 * DTOs pour la revue KYC par un Admin (backoffice web) — complément des
 * endpoints mobiles /api/kyc/* sans les modifier.
 */
public class KycAdminDto {

    public record ActeurSummary(
            UUID acteurId,
            String telephone,
            String nom,
            String prenom,
            String raisonSociale,
            String niveauKyc,
            Set<RoleActeur> roles
    ) {
        public static ActeurSummary from(Acteur acteur) {
            return new ActeurSummary(
                    acteur.getId(),
                    acteur.getTelephone(),
                    acteur.getNom(),
                    acteur.getPrenom(),
                    acteur.getRaisonSociale(),
                    acteur.getNiveauKyc().name(),
                    acteur.getRoles());
        }
    }

    /** Détail revue Admin : identité + rôles + pièces (URLs présignées). */
    public record ActeurDetail(
            UUID acteurId,
            String telephone,
            String nom,
            String prenom,
            String raisonSociale,
            String niveauKyc,
            Set<RoleActeur> roles,
            java.util.List<KycDto.PieceResponse> pieces
    ) {
        public static ActeurDetail from(Acteur acteur, java.util.List<KycDto.PieceResponse> pieces) {
            return new ActeurDetail(
                    acteur.getId(),
                    acteur.getTelephone(),
                    acteur.getNom(),
                    acteur.getPrenom(),
                    acteur.getRaisonSociale(),
                    acteur.getNiveauKyc().name(),
                    acteur.getRoles(),
                    pieces);
        }
    }

    @Data
    public static class DecisionRequest {
        @NotBlank
        @Pattern(regexp = "VALIDE|REJETE", message = "decision doit être VALIDE ou REJETE")
        private String decision;

        /** Optionnel — journalisé côté serveur faute de champ dédié en base. */
        private String motif;
    }

    public enum Decision {
        VALIDE,
        REJETE
    }
}
