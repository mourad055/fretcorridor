package com.fretcorridor.gateway.domain.opt;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * UC-MAT-02 du CDC (page 43) : proposition de mission en attente de reponse
 * du chauffeur/transporteur. Champs dans l'ordre d'affichage exige par
 * RG-049 ("la remuneration est la premiere affichee").
 */
public record PropositionMissionCandidate(
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
}
