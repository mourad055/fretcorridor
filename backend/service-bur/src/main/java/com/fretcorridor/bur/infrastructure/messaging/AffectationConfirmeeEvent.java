package com.fretcorridor.bur.infrastructure.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Miroir exact du contrat shared-contracts/asyncapi/events/affectation-confirmee.yaml
 * (OPT, Moteur → EXE, Mobile — consommé ici aussi par Bureau/service-bur pour
 * matérialiser la vue Bureau des missions appariées, EF-BUR-01/02).
 */
public record AffectationConfirmeeEvent(
        UUID eventId,
        UUID missionId,
        UUID demandeId,
        UUID capaciteId,
        UUID vehiculeId,
        UUID transporteurId,
        UUID chargeurId,
        UUID axeId,
        Double origineLatitude,
        Double origineLongitude,
        String origineNom,
        Double destinationLatitude,
        Double destinationLongitude,
        String destinationNom,
        Double distanceEstimeeMetres,
        Integer dureeEstimeeSecondes,
        Integer intervalleConfianceSecondes,
        String geometrieEncodee,
        BigDecimal prixTransport,
        BigDecimal commissionPlateforme,
        BigDecimal montantVerseTransporteur,
        String devise,
        String modeCollecte,
        String modeRemise,
        Instant horodatageConfirmation
) {
}
