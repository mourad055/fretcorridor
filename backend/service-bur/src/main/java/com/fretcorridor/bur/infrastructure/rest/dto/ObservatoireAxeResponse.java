package com.fretcorridor.bur.infrastructure.rest.dto;

import com.fretcorridor.bur.domain.ObservatoireAxe;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Les champs indicateurs sont {@code null} tant que le seuil d'agrégation
 * n'est pas atteint (EF-BUR-04). {@code couverturePourcentage} et
 * {@code estimationDefinieLe} restent {@code null} tant qu'aucune estimation
 * de marché n'a été déclarée pour l'axe (EF-BUR-05, RG-087) — pas déduits.
 */
public record ObservatoireAxeResponse(
        String axeId,
        long seuil,
        boolean seuilAtteint,
        Long nombreMissions,
        BigDecimal prixMediane,
        BigDecimal prixDispersion,
        String devise,
        Double tauxDesequilibreDirectionnel,
        BigDecimal couverturePourcentage,
        Instant estimationDefinieLe
) {
    public static ObservatoireAxeResponse from(ObservatoireAxe observatoire) {
        return new ObservatoireAxeResponse(
                observatoire.axeId().toString(),
                observatoire.seuil(),
                observatoire.seuilAtteint(),
                observatoire.nombreMissions().orElse(null),
                observatoire.prixMediane().orElse(null),
                observatoire.prixDispersion().orElse(null),
                observatoire.devise().orElse(null),
                observatoire.tauxDesequilibreDirectionnel().orElse(null),
                observatoire.couverturePourcentage().orElse(null),
                observatoire.estimationDefinieLe().orElse(null)
        );
    }
}
