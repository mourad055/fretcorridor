package com.fretcorridor.bur.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** EF-BUR-07 (S), UC-BUR-02 A1 : alerte de marché configurée par un agent sur un indicateur de l'observatoire d'un axe. */
public record AlerteSeuil(
        String id,
        String tenantId,
        UUID axeId,
        IndicateurObservatoire indicateur,
        Comparateur comparateur,
        BigDecimal seuil,
        String creeParActeurId,
        Instant creeLe
) {
}
