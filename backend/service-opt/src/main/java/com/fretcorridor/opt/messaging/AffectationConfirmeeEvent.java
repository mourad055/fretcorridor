package com.fretcorridor.opt.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AffectationConfirmeeEvent(
        UUID eventId,
        UUID missionId,
        UUID demandeId,
        UUID capaciteId,
        UUID vehiculeId,
        UUID transporteurId,
        UUID chargeurId,
        UUID axeId,
        double origineLatitude,
        double origineLongitude,
        String origineNom,
        double destinationLatitude,
        double destinationLongitude,
        String destinationNom,
        Double distanceEstimeeMetres,
        Long dureeEstimeeSecondes,
        Long intervalleConfianceSecondes,
        String geometrieEncodee,
        BigDecimal prixTransport,
        BigDecimal commissionPlateforme,
        BigDecimal montantVerseTransporteur,
        String devise,
        String modeCollecte,
        String modeRemise,
        Instant horodatageConfirmation
) {}
