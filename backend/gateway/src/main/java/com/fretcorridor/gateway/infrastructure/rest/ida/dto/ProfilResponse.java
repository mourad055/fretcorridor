package com.fretcorridor.gateway.infrastructure.rest.ida.dto;

import com.fretcorridor.gateway.domain.ida.Profil;

public record ProfilResponse(String acteurId, String type, String nom, String prenom,
                              String raisonSociale, String niveauKyc) {
    public static ProfilResponse from(Profil profil) {
        return new ProfilResponse(profil.acteurId(), profil.type(), profil.nom(), profil.prenom(),
                profil.raisonSociale(), profil.niveauKyc());
    }
}
