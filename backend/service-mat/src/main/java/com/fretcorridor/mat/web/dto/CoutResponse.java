package com.fretcorridor.mat.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Resultat de cout pour un candidat (capacite) donne au sein d'un lot.
 */
public record CoutResponse(
        UUID capaciteId,
        UUID cycleMatchingId,
        BigDecimal coutTotal
) {
}
