package com.fretcorridor.pay.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * EF-PAY-07 (S) : mode de paiement en espèces à l'enlèvement (CDC §7.6,
 * UC-PAY-01 A3) — « mode dégradé, marqué comme tel, sans séquestre et sans
 * garantie de la plateforme ». Ce n'est ni une {@link Sequestre} ni une
 * {@link Garantie} : aucun fonds ne transite par un prestataire, donc aucune
 * écriture de grand livre n'est créée pour ce montant, et il ne contribue
 * jamais au pool de fonds disponibles pour un reversement.
 */
public record DeclarationEspeces(
        String id,
        String tenantId,
        String missionId,
        BigDecimal montant,
        Instant declareeLe
) {
    public DeclarationEspeces {
        if (montant == null || montant.signum() <= 0) {
            throw new IllegalArgumentException("Le montant d'une déclaration espèces doit être strictement positif");
        }
    }
}
