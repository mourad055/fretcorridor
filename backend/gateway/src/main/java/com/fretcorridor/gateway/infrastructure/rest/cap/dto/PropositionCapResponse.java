package com.fretcorridor.gateway.infrastructure.rest.cap.dto;

import com.fretcorridor.gateway.domain.cap.PropositionCap;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PropositionCapResponse(
        UUID affectationId, UUID demandeId, UUID capaciteId, String statut,
        String origineNom, String destinationNom, Double distanceMetres, Double dureeSecondes,
        BigDecimal prixTransport, Instant expireA, Instant dateCreation
) {
    public static PropositionCapResponse from(PropositionCap p) {
        return new PropositionCapResponse(p.affectationId(), p.demandeId(), p.capaciteId(), p.statut(),
                p.origineNom(), p.destinationNom(), p.distanceMetres(), p.dureeSecondes(),
                p.prixTransport(), p.expireA(), p.dateCreation());
    }
}
