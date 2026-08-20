package com.fretcorridor.gateway.infrastructure.rest.kyc.dto;

import com.fretcorridor.gateway.domain.kyc.KycDossier;

import java.time.Instant;

public record KycDossierResponse(
        String id,
        String acteurNom,
        String acteurTelephone,
        String typeActeur,
        Instant soumisLe,
        String statut
) {
    public static KycDossierResponse from(KycDossier dossier) {
        return new KycDossierResponse(
                dossier.id(),
                dossier.acteurNom(),
                dossier.acteurTelephone(),
                dossier.typeActeur(),
                dossier.soumisLe(),
                dossier.statut().name()
        );
    }
}
