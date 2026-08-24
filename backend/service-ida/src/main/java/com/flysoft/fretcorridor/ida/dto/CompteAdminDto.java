package com.flysoft.fretcorridor.ida.dto;

import com.flysoft.fretcorridor.ida.entity.Acteur;
import com.flysoft.fretcorridor.ida.entity.RoleActeur;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

/**
 * Gestion des comptes par un Admin (audit UX 2026-08-23,
 * docs/AUDIT_ROADMAP_Backoffice_Web_2026-08-23.md §1.1) : jusqu'ici aucune
 * interface n'exposait le cycle de vie d'un compte (Acteur) au-delà de son
 * propre profil (KYC) ou de son enrôlement initial par un agent terrain.
 */
public class CompteAdminDto {

    public record CompteResponse(
            UUID id,
            String telephone,
            String nom,
            String prenom,
            String raisonSociale,
            String tenantId,
            Set<RoleActeur> roles,
            boolean actif,
            String niveauKyc
    ) {
        public static CompteResponse from(Acteur acteur) {
            return new CompteResponse(acteur.getId(), acteur.getTelephone(), acteur.getNom(), acteur.getPrenom(),
                    acteur.getRaisonSociale(), acteur.getTenantId(), acteur.getRoles(), acteur.getActif(),
                    acteur.getNiveauKyc().name());
        }
    }

    @Data
    public static class ChangerStatutRequest {
        @NotNull private Boolean actif;
    }

    @Data
    public static class ChangerRolesRequest {
        @NotEmpty(message = "un compte doit conserver au moins un rôle")
        private Set<RoleActeur> roles;
    }
}
