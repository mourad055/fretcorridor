package com.flysoft.fretcorridor.mkt.messaging;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// Miroir exact de PropositionEmiseEvent cote service-opt (Moteur) - contrat
// shared-contracts/asyncapi/events/proposition-emise.yaml.
public record PropositionEmiseEvent(
        UUID eventId,
        UUID cycleMatchingId,
        UUID demandeId,
        UUID capaciteId,
        UUID missionId,
        UUID axeId,
        Integer rang,
        String motifClassement,
        BigDecimal prixTransport,
        BigDecimal commissionPlateforme,
        String devise,
        Double distanceEstimeeMetres,
        Integer dureeEstimeeSecondes,
        String origineNom,
        String destinationNom,
        LocalDateTime horodatageEmission
) {
}
