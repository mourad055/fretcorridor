package com.flysoft.fretcorridor.cap.web.dto;

import com.flysoft.fretcorridor.cap.client.AffectationProposeeDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** UC-MAT-02/diffusion-course : reponse "mes propositions" cote app Chauffeur. */
public record PropositionCapResponse(
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
    public static PropositionCapResponse from(AffectationProposeeDto dto) {
        return new PropositionCapResponse(
                dto.missionId(), dto.demandeId(), dto.capaciteId(), dto.statut(),
                dto.origineNom(), dto.destinationNom(), dto.distanceMetres(), dto.dureeSecondes(),
                dto.prixTransport(), dto.expireA(), dto.dateCreation());
    }
}
