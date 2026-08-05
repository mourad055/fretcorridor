package com.fretcorridor.pay.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Grand livre en partie double (CDC §13). Une écriture ne référence jamais un
 * compte FretCorridor — voir {@link TypeCompte} (ENF-FIN-01).
 *
 * {@code tenantId} porte l'isolation territoriale du rapport financier Bureau
 * (ENF-MUL-01). {@code beneficiaireId} identifie l'acteur tiers concerné
 * (transporteur ou chargeur) pour les écritures COMPTE_TRANSPORTEUR/COMPTE_CHARGEUR
 * — null pour un encaissement au séquestre du prestataire.
 */
public record EcritureMiroir(
        String id,
        String tenantId,
        String missionId,
        TypeCompte typeCompte,
        String beneficiaireId,
        SensEcriture sens,
        NatureEcriture nature,
        BigDecimal montant,
        String referencePrestataire,
        Instant creeLe,
        StatutEcriture statut
) {
    public EcritureMiroir {
        if (montant == null || montant.signum() <= 0) {
            throw new IllegalArgumentException("Le montant d'une écriture doit être strictement positif");
        }
    }

    public EcritureMiroir suspendue() {
        return new EcritureMiroir(id, tenantId, missionId, typeCompte, beneficiaireId, sens, nature, montant, referencePrestataire, creeLe, StatutEcriture.SUSPENDUE);
    }
}
