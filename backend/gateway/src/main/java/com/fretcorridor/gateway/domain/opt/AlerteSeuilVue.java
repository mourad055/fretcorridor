package com.fretcorridor.gateway.domain.opt;

import java.math.BigDecimal;
import java.time.Instant;

/** EF-BUR-07 (S) : alerte sur seuil configurée par un agent Bureau sur un indicateur de l'observatoire. */
public record AlerteSeuilVue(
        String id,
        String axeId,
        String indicateur,
        String comparateur,
        BigDecimal seuil,
        String creeParActeurId,
        Instant creeLe
) {
}
