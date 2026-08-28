package com.fretcorridor.gateway.domain.kyc;

import java.time.Instant;
import java.util.Set;

/**
 * Vue d'un dossier KYC pour le dashboard Admin. Les pièces et URLs
 * présignées vivent dans {@link KycDetail} (GET détail).
 */
public record KycDossier(
        String id,
        String acteurNom,
        String acteurTelephone,
        String typeActeur,
        Instant soumisLe,
        KycStatut statut,
        String niveauKyc,
        Set<String> roles
) {
    public KycDossier avecStatut(KycStatut nouveauStatut) {
        return new KycDossier(id, acteurNom, acteurTelephone, typeActeur, soumisLe, nouveauStatut, niveauKyc, roles);
    }

    public KycDossier avecNiveau(String nouveauNiveau, KycStatut nouveauStatut) {
        return new KycDossier(id, acteurNom, acteurTelephone, typeActeur, soumisLe, nouveauStatut, nouveauNiveau, roles);
    }
}
