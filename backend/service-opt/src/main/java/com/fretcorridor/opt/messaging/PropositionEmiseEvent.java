package com.fretcorridor.opt.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PropositionEmiseEvent(
        UUID eventId,
        UUID cycleMatchingId,
        UUID demandeId,
        UUID capaciteId,
        UUID missionId,
        UUID axeId,
        int rang,
        String motifClassement,
        BigDecimal prixTransport,
        BigDecimal commissionPlateforme,
        String devise,
        double distanceEstimeeMetres,
        Long dureeEstimeeSecondes,
        String origineNom,
        String destinationNom,
        Instant horodatageEmission
) {}
