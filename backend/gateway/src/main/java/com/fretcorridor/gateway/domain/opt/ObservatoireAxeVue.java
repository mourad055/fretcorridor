package com.fretcorridor.gateway.domain.opt;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * EF-BUR-03, UC-BUR-02 : indicateurs de marché d'un axe (volumes, prix
 * médian et dispersion, déséquilibre directionnel). Champs indicateurs
 * {@code null} tant que le seuil d'agrégation n'est pas atteint (EF-BUR-04,
 * RG-085) — jamais de donnée ré-identifiante exposée en dessous.
 *
 * {@code couverturePourcentage}/{@code estimationDefinieLe} restent
 * {@code null} tant qu'aucune estimation de marché n'a été déclarée pour
 * l'axe (EF-BUR-05, RG-087).
 */
public record ObservatoireAxeVue(
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
}
