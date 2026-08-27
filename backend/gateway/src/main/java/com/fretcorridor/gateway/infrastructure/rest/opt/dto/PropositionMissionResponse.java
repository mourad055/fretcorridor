package com.fretcorridor.gateway.infrastructure.rest.opt.dto;

import com.fretcorridor.gateway.domain.opt.PropositionMissionCandidate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** UC-MAT-02 : champs dans l'ordre exige par RG-049 (remuneration en premier). */
public record PropositionMissionResponse(
        UUID id,
        UUID demandeId,
        BigDecimal prixTransport,
        String origineNom,
        String destinationNom,
        String typeEmballageNom,
        Integer quantite,
        Double poidsTotalKg,
        String destinataireNom,
        String destinataireTelephone,
        String modeCollecte,
        String typeDisponibilite,
        Double distanceMetres,
        Double dureeSecondes,
        Boolean grandeValeur,
        String statut,
        Instant expireA,
        Instant dateCreation
) {
    public static PropositionMissionResponse from(PropositionMissionCandidate p) {
        return new PropositionMissionResponse(
                p.id(), p.demandeId(), p.prixTransport(), p.origineNom(), p.destinationNom(),
                p.typeEmballageNom(), p.quantite(), p.poidsTotalKg(), p.destinataireNom(),
                p.destinataireTelephone(), p.modeCollecte(), p.typeDisponibilite(),
                p.distanceMetres(), p.dureeSecondes(), p.grandeValeur(), p.statut(),
                p.expireA(), p.dateCreation());
    }
}
