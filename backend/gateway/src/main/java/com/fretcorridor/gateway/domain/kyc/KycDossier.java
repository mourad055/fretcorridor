package com.fretcorridor.gateway.domain.kyc;

import java.time.Instant;

/**
 * Vue minimale d'un dossier KYC telle qu'exposée au dashboard Admin (Sprint 2).
 * Le modèle complet (pièces, extraction automatique, niveaux 0-1-2) appartient à
 * service-ida (Mobile, cf. CDC UC-IDA-01) — ce dashboard n'en consomme qu'un
 * sous-ensemble de décision.
 */
public record KycDossier(
        String id,
        String acteurNom,
        String acteurTelephone,
        String typeActeur,
        Instant soumisLe,
        KycStatut statut
) {
    public KycDossier avecStatut(KycStatut nouveauStatut) {
        return new KycDossier(id, acteurNom, acteurTelephone, typeActeur, soumisLe, nouveauStatut);
    }
}
