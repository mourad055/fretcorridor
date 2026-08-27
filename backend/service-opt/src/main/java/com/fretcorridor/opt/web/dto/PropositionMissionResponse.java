package com.fretcorridor.opt.web.dto;

import com.fretcorridor.opt.domain.PropositionMission;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * UC-MAT-02 du CDC : champs dans l'ordre d'affichage exige par RG-049
 * ("la remuneration est la premiere affichee") -- prixTransport en tete du
 * record est une convention de lecture, l'ordre reel d'affichage reste la
 * responsabilite de l'app mobile, mais ce record documente l'intention.
 */
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
    public static PropositionMissionResponse from(PropositionMission p) {
        return new PropositionMissionResponse(
                p.getId(),
                p.getDemandeId(),
                p.getPrixTransport(),
                p.getOrigineNom(),
                p.getDestinationNom(),
                p.getTypeEmballageNom(),
                p.getQuantite(),
                p.getPoidsTotalKg(),
                p.getDestinataireNom(),
                p.getDestinataireTelephone(),
                p.getModeCollecte(),
                p.getTypeDisponibilite(),
                p.getDistanceMetres(),
                p.getDureeSecondes(),
                p.getGrandeValeur(),
                p.getStatut().name(),
                p.getExpireA(),
                p.getDateCreation()
        );
    }
}
