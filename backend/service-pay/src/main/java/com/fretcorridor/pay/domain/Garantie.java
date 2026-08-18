package com.fretcorridor.pay.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * EF-PAY-06 (terme contractuel) : garantie souscrite auprès d'un tiers
 * garant (CDC §7.6, UC-PAY-01 A1) qui autorise la confirmation d'une mission
 * sans encaissement préalable. Le risque de crédit est porté par
 * {@code garantId}, jamais par FretCorridor — cette classe ne modélise
 * aucun compte au sens de {@link TypeCompte} et n'est pas une écriture de
 * grand livre : c'est un engagement de tiers, distinct de l'encaissement
 * réel, que {@link GrandLivreService#enregistrerReversement} traite comme un
 * substitut valable à l'encaissement pour la vérification RG-075. 1-1 avec
 * une mission (même patron que {@link Sequestre}).
 */
public record Garantie(
        String id,
        String tenantId,
        String missionId,
        String garantId,
        BigDecimal montant,
        String referenceGarantie,
        Instant engageeLe
) {
    public Garantie {
        if (montant == null || montant.signum() <= 0) {
            throw new IllegalArgumentException("Le montant d'une garantie doit être strictement positif");
        }
    }
}
