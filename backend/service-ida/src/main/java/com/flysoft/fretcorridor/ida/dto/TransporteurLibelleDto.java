package com.flysoft.fretcorridor.ida.dto;

import com.flysoft.fretcorridor.ida.entity.Acteur;

import java.util.UUID;

public record TransporteurLibelleDto(UUID acteurId, String libelle) {

    public static TransporteurLibelleDto from(Acteur acteur) {
        return new TransporteurLibelleDto(acteur.getId(), nomAffiche(acteur));
    }

    private static String nomAffiche(Acteur acteur) {
        if (acteur.getRaisonSociale() != null && !acteur.getRaisonSociale().isBlank()) {
            return acteur.getRaisonSociale();
        }
        String nom = acteur.getNom() == null ? "" : acteur.getNom().trim();
        String prenom = acteur.getPrenom() == null ? "" : acteur.getPrenom().trim();
        String compose = (nom + " " + prenom).trim();
        return compose.isEmpty() ? acteur.getTelephone() : compose;
    }
}
