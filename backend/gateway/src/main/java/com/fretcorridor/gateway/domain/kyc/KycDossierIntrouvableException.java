package com.fretcorridor.gateway.domain.kyc;

public class KycDossierIntrouvableException extends RuntimeException {

    public KycDossierIntrouvableException(String dossierId) {
        super("Aucun dossier KYC avec l'identifiant " + dossierId);
    }
}
