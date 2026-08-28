package com.fretcorridor.gateway.infrastructure.rest.kyc.dto;

import com.fretcorridor.gateway.domain.kyc.KycDossier;

import java.time.Instant;
import java.util.Set;

public record KycDossierResponse(
        String id,
        String acteurNom,
        String acteurTelephone,
        String typeActeur,
        Instant soumisLe,
        String statut,
        String niveauKyc,
        Set<String> roles
) {
    public static KycDossierResponse from(KycDossier dossier) {
        return new KycDossierResponse(
                dossier.id(),
                dossier.acteurNom(),
                dossier.acteurTelephone(),
                dossier.typeActeur(),
                dossier.soumisLe(),
                dossier.statut().name(),
                dossier.niveauKyc(),
                dossier.roles()
        );
    }
}
