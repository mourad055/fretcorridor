package com.fretcorridor.gateway.domain.opt;

import java.math.BigDecimal;

/**
 * EF-BUR-03, UC-BUR-02 : indicateurs de marché d'un axe (volumes, prix
 * médian et dispersion, déséquilibre directionnel). Champs indicateurs
 * {@code null} tant que le seuil d'agrégation n'est pas atteint (EF-BUR-04,
 * RG-085) — jamais de donnée ré-identifiante exposée en dessous.
 */
public record ObservatoireAxeVue(
        String axeId,
        long seuil,
        boolean seuilAtteint,
        Long nombreMissions,
        BigDecimal prixMediane,
        BigDecimal prixDispersion,
        String devise,
        Double tauxDesequilibreDirectionnel
) {
}
