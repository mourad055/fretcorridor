package com.fretcorridor.gateway.infrastructure.rest.ida.dto;

import com.fretcorridor.gateway.domain.ida.CompteAdmin;

import java.util.Set;

public record CompteAdminResponse(
        String id,
        String telephone,
        String nom,
        String prenom,
        String raisonSociale,
        String tenantId,
        Set<String> roles,
        boolean actif,
        String niveauKyc
) {
    public static CompteAdminResponse from(CompteAdmin compte) {
        return new CompteAdminResponse(compte.id(), compte.telephone(), compte.nom(), compte.prenom(),
                compte.raisonSociale(), compte.tenantId(), compte.roles(), compte.actif(), compte.niveauKyc());
    }
}
