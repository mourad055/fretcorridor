package com.fretcorridor.gateway.domain.cap;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** UC-MAT-02/diffusion-course : une proposition en attente pour le transporteur connecte. */
public record PropositionCap(
        UUID affectationId,
        UUID demandeId,
        UUID capaciteId,
        String statut,
        String origineNom,
        String destinationNom,
        Double distanceMetres,
        Double dureeSecondes,
        BigDecimal prixTransport,
        Instant expireA,
        Instant dateCreation
) {
}
